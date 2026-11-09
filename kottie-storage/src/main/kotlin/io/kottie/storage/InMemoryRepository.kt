package io.kottie.storage

import io.kottie.api.storage.Repository
import io.kottie.api.storage.Transactional
import io.kottie.api.storage.Versioned
import io.kottie.api.storage.VersionedRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Concurrent in-memory repository for tests and single-process use.
 */
class InMemoryRepository<K : Any, V : Any> : Repository<K, V>, Transactional {

    private val map = ConcurrentHashMap<K, V>()
    private val mutex = Mutex()

    override suspend fun find(key: K): V? = map[key]

    override suspend fun save(key: K, value: V) {
        map[key] = value
    }

    override suspend fun delete(key: K): Boolean = map.remove(key) != null

    override suspend fun exists(key: K): Boolean = map.containsKey(key)

    override suspend fun <T> transaction(block: suspend () -> T): T =
        mutex.withLock { block() }

    fun clear() = map.clear()
    fun size(): Int = map.size
}

/**
 * In-memory versioned repository with optimistic concurrency.
 */
class InMemoryVersionedRepository<K : Any, V : Any> : VersionedRepository<K, V>, Transactional {

    private data class Entry<V>(val value: V, val version: Long)

    private val map = ConcurrentHashMap<K, Entry<V>>()
    private val mutex = Mutex()

    override suspend fun find(key: K): V? = map[key]?.value

    override suspend fun findVersioned(key: K): Versioned<V>? =
        map[key]?.let { Versioned(it.value, it.version) }

    override suspend fun save(key: K, value: V) {
        map.compute(key) { _, old ->
            Entry(value, (old?.version ?: 0L) + 1L)
        }
    }

    override suspend fun saveIfVersion(key: K, value: V, expectedVersion: Long): Boolean {
        var success = false
        map.compute(key) { _, old ->
            when {
                old == null && expectedVersion == 0L -> {
                    success = true
                    Entry(value, 1L)
                }
                old != null && old.version == expectedVersion -> {
                    success = true
                    Entry(value, old.version + 1L)
                }
                else -> old
            }
        }
        return success
    }

    override suspend fun delete(key: K): Boolean = map.remove(key) != null

    override suspend fun exists(key: K): Boolean = map.containsKey(key)

    override suspend fun <T> transaction(block: suspend () -> T): T =
        mutex.withLock { block() }
}
