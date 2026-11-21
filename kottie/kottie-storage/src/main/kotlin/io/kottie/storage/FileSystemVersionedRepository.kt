package io.kottie.storage

import io.kottie.api.serialization.PayloadCodec
import io.kottie.api.storage.Transactional
import io.kottie.api.storage.Versioned
import io.kottie.api.storage.VersionedRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Disk-backed [VersionedRepository] with optimistic concurrency.
 *
 * File layout per key: 8-byte big-endian version + codec payload.
 * Writes are atomic (temp + move). Keys are SHA-256 hashed (no path traversal).
 */
class FileSystemVersionedRepository<V : Any>(
    private val rootDir: Path,
    private val codec: PayloadCodec<V>
) : VersionedRepository<String, V>, Transactional {

    private val global = Mutex()
    private val keyLocks = ConcurrentHashMap<String, Mutex>()

    init {
        Files.createDirectories(rootDir)
    }

    override suspend fun find(key: String): V? =
        findVersioned(key)?.value

    override suspend fun findVersioned(key: String): Versioned<V>? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        if (!Files.isRegularFile(file)) return@withContext null
        val all = Files.readAllBytes(file)
        require(all.size >= 8) { "corrupt versioned file for $key" }
        val buf = ByteBuffer.wrap(all)
        val version = buf.long
        val payload = ByteArray(all.size - 8)
        buf.get(payload)
        if (payload.size > codec.maxPayloadBytes) {
            error("Stored payload for $key exceeds maxPayloadBytes")
        }
        Versioned(codec.decode(payload), version)
    }

    override suspend fun save(key: String, value: V) {
        val current = findVersioned(key)
        val expected = current?.version ?: 0L
        saveIfVersion(key, value, expected)
    }

    override suspend fun saveIfVersion(key: String, value: V, expectedVersion: Long): Boolean =
        withContext(Dispatchers.IO) {
            lockFor(key).withLock {
                val file = fileFor(key)
                val currentVersion = if (Files.isRegularFile(file)) {
                    val all = Files.readAllBytes(file)
                    if (all.size >= 8) ByteBuffer.wrap(all).long else -1L
                } else {
                    0L
                }
                if (currentVersion != expectedVersion) return@withLock false

                val payload = codec.encode(value)
                if (payload.size > codec.maxPayloadBytes) {
                    error("Encoded payload exceeds maxPayloadBytes")
                }
                val nextVersion = expectedVersion + 1L
                val out = ByteBuffer.allocate(8 + payload.size)
                out.putLong(nextVersion)
                out.put(payload)
                Files.createDirectories(file.parent)
                val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
                Files.write(
                    tmp,
                    out.array(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
                )
                Files.move(
                    tmp,
                    file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
                true
            }
        }

    override suspend fun delete(key: String): Boolean = withContext(Dispatchers.IO) {
        lockFor(key).withLock {
            Files.deleteIfExists(fileFor(key))
        }
    }

    override suspend fun exists(key: String): Boolean = withContext(Dispatchers.IO) {
        Files.isRegularFile(fileFor(key))
    }

    override suspend fun <T> transaction(block: suspend () -> T): T =
        global.withLock { block() }

    private fun lockFor(key: String): Mutex =
        keyLocks.computeIfAbsent(key) { Mutex() }

    private fun fileFor(key: String): Path {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
        val name = digest.joinToString("") { "%02x".format(it) }
        return rootDir.resolve("$name.vbin")
    }
}
