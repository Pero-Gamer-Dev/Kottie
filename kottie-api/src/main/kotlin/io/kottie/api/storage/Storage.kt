package io.kottie.api.storage

/**
 * Load/save domain data by a stable key.
 * Implementations must not store live server objects — only domain snapshots.
 */
interface Repository<K : Any, V : Any> {
    suspend fun find(key: K): V?
    suspend fun save(key: K, value: V)
    suspend fun delete(key: K): Boolean
    suspend fun exists(key: K): Boolean
}

/**
 * Optional multi-step atomic boundary where the backend supports it.
 * In-memory and simple backends may execute the block without real transactions.
 */
interface Transactional {
    suspend fun <T> transaction(block: suspend () -> T): T
}

/**
 * Explicit cache invalidation contract (optional layer on top of a repository).
 */
interface CacheInvalidator<K : Any> {
    fun invalidate(key: K)
    fun invalidateAll()
}

/**
 * Conflict policy when two writers update the same key.
 */
enum class ConflictPolicy {
    /** Last write wins (no version check). */
    LAST_WRITE_WINS,
    /** Reject update if expected version does not match. */
    VERSION_CHECK,
    /** Domain-specific merge (implementation-defined). */
    MERGE
}

/**
 * Versioned value for optimistic concurrency.
 */
data class Versioned<V>(
    val value: V,
    val version: Long
)

interface VersionedRepository<K : Any, V : Any> : Repository<K, V> {
    suspend fun findVersioned(key: K): Versioned<V>?
    /**
     * Save only if the current version matches [expectedVersion].
     * Returns false on conflict.
     */
    suspend fun saveIfVersion(key: K, value: V, expectedVersion: Long): Boolean
}
