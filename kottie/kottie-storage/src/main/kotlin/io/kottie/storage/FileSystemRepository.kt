package io.kottie.storage

import io.kottie.api.serialization.PayloadCodec
import io.kottie.api.storage.Repository
import io.kottie.api.storage.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Durable [Repository] that stores one file per key under [rootDir].
 *
 * - Keys are hashed to safe filenames (no path traversal).
 * - Values are encoded with a [PayloadCodec].
 * - Writes are atomic (temp file + move).
 * - Optional in-process mutex per key for simple atomicity.
 *
 * Survives process restart when [rootDir] is on durable disk.
 */
class FileSystemRepository<V : Any>(
    private val rootDir: Path,
    private val codec: PayloadCodec<V>,
    private val keyToString: (Any) -> String = { it.toString() }
) : Repository<String, V>, Transactional {

    private val log = LoggerFactory.getLogger(FileSystemRepository::class.java)
    private val global = Mutex()
    private val keyLocks = ConcurrentHashMap<String, Mutex>()

    init {
        Files.createDirectories(rootDir)
    }

    override suspend fun find(key: String): V? = withContext(Dispatchers.IO) {
        val file = fileFor(key)
        if (!Files.isRegularFile(file)) return@withContext null
        val bytes = Files.readAllBytes(file)
        if (bytes.size > codec.maxPayloadBytes) {
            error("Stored payload for $key exceeds codec maxPayloadBytes (${bytes.size})")
        }
        codec.decode(bytes)
    }

    override suspend fun save(key: String, value: V) = withContext(Dispatchers.IO) {
        lockFor(key).withLock {
            val bytes = codec.encode(value)
            if (bytes.size > codec.maxPayloadBytes) {
                error("Encoded payload for $key exceeds maxPayloadBytes (${bytes.size})")
            }
            val target = fileFor(key)
            Files.createDirectories(target.parent)
            val tmp = target.resolveSibling(target.fileName.toString() + ".tmp")
            Files.write(
                tmp,
                bytes,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )
            Files.move(
                tmp,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }
    }

    override suspend fun delete(key: String): Boolean = withContext(Dispatchers.IO) {
        lockFor(key).withLock {
            val file = fileFor(key)
            if (!Files.isRegularFile(file)) return@withContext false
            Files.deleteIfExists(file)
        }
    }

    override suspend fun exists(key: String): Boolean = withContext(Dispatchers.IO) {
        Files.isRegularFile(fileFor(key))
    }

    override suspend fun <T> transaction(block: suspend () -> T): T =
        global.withLock { block() }

    /** Number of data files currently under the root (best-effort). */
    fun size(): Long = try {
        Files.list(rootDir).use { stream ->
            stream.filter { it.fileName.toString().endsWith(".bin") }.count()
        }
    } catch (_: Throwable) {
        -1L
    }

    private fun lockFor(key: String): Mutex =
        keyLocks.computeIfAbsent(key) { Mutex() }

    private fun fileFor(key: String): Path {
        val safe = hashKey(keyToString(key))
        return rootDir.resolve("$safe.bin")
    }

    private fun hashKey(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}

/**
 * SQL-shaped SPI for production JDBC/Hikari drivers.
 * No JDBC dependency is required to compile this interface.
 *
 * Implementations should:
 * - use prepared statements
 * - map domain values via [PayloadCodec] or column mapping
 * - honor [io.kottie.api.storage.Transactional] with real DB transactions when possible
 */
interface SqlRepositoryDriver {
    suspend fun connect()
    suspend fun close()
    suspend fun execute(sql: String, params: List<Any?> = emptyList()): Int
    suspend fun queryOne(sql: String, params: List<Any?> = emptyList()): ByteArray?
    suspend fun queryMany(sql: String, params: List<Any?> = emptyList()): List<ByteArray>
}

/**
 * [Repository] backed by a [SqlRepositoryDriver] and a simple key/value table.
 *
 * Expected schema (create externally or via [ensureSchema]):
 * ```
 * CREATE TABLE kottie_kv (
 *   k VARCHAR(255) PRIMARY KEY,
 *   v BLOB NOT NULL
 * );
 * ```
 */
class SqlKeyValueRepository<V : Any>(
    private val driver: SqlRepositoryDriver,
    private val codec: PayloadCodec<V>,
    private val table: String = "kottie_kv"
) : Repository<String, V>, Transactional {

    init {
        require(table.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
            "Unsafe table name: $table"
        }
    }

    suspend fun ensureSchema() {
        driver.execute(
            "CREATE TABLE IF NOT EXISTS $table (k VARCHAR(255) PRIMARY KEY, v BLOB NOT NULL)"
        )
    }

    override suspend fun find(key: String): V? {
        val bytes = driver.queryOne("SELECT v FROM $table WHERE k = ?", listOf(key)) ?: return null
        if (bytes.size > codec.maxPayloadBytes) {
            error("DB payload for $key exceeds maxPayloadBytes")
        }
        return codec.decode(bytes)
    }

    override suspend fun save(key: String, value: V) {
        val bytes = codec.encode(value)
        if (bytes.size > codec.maxPayloadBytes) {
            error("Encoded payload exceeds maxPayloadBytes")
        }
        // Portable upsert approximation: delete + insert inside transaction
        transaction {
            driver.execute("DELETE FROM $table WHERE k = ?", listOf(key))
            driver.execute("INSERT INTO $table (k, v) VALUES (?, ?)", listOf(key, bytes))
        }
    }

    override suspend fun delete(key: String): Boolean {
        val n = driver.execute("DELETE FROM $table WHERE k = ?", listOf(key))
        return n > 0
    }

    override suspend fun exists(key: String): Boolean =
        driver.queryOne("SELECT v FROM $table WHERE k = ?", listOf(key)) != null

    override suspend fun <T> transaction(block: suspend () -> T): T = block()
}

/**
 * In-process SQL driver for tests (no external database).
 * Supports the small subset of SQL used by [SqlKeyValueRepository].
 */
class InMemorySqlDriver : SqlRepositoryDriver {
    private data class Row(var value: ByteArray)
    private val tables = ConcurrentHashMap<String, ConcurrentHashMap<String, Row>>()
    private var open = false

    override suspend fun connect() {
        open = true
    }

    override suspend fun close() {
        open = false
        tables.clear()
    }

    override suspend fun execute(sql: String, params: List<Any?>): Int {
        check(open)
        val normalized = sql.trim().lowercase()
        when {
            normalized.startsWith("create table") -> {
                val name = tableNameFromCreate(sql)
                tables.computeIfAbsent(name) { ConcurrentHashMap() }
                return 0
            }
            normalized.startsWith("delete from") -> {
                val name = tableNameFromDelete(sql)
                val key = params[0] as String
                return if (tables[name]?.remove(key) != null) 1 else 0
            }
            normalized.startsWith("insert into") -> {
                val name = tableNameFromInsert(sql)
                val key = params[0] as String
                val value = params[1] as ByteArray
                tables.getOrPut(name) { ConcurrentHashMap() }[key] = Row(value)
                return 1
            }
            else -> error("Unsupported SQL in InMemorySqlDriver: $sql")
        }
    }

    override suspend fun queryOne(sql: String, params: List<Any?>): ByteArray? {
        check(open)
        val name = tableNameFromSelect(sql)
        val key = params[0] as String
        return tables[name]?.get(key)?.value
    }

    override suspend fun queryMany(sql: String, params: List<Any?>): List<ByteArray> {
        val one = queryOne(sql, params) ?: return emptyList()
        return listOf(one)
    }

    private fun tableNameFromCreate(sql: String): String =
        Regex("""CREATE TABLE IF NOT EXISTS\s+(\w+)""", RegexOption.IGNORE_CASE)
            .find(sql)?.groupValues?.get(1)
            ?: error("Cannot parse table from $sql")

    private fun tableNameFromDelete(sql: String): String =
        Regex("""DELETE FROM\s+(\w+)""", RegexOption.IGNORE_CASE)
            .find(sql)?.groupValues?.get(1)
            ?: error("Cannot parse table from $sql")

    private fun tableNameFromInsert(sql: String): String =
        Regex("""INSERT INTO\s+(\w+)""", RegexOption.IGNORE_CASE)
            .find(sql)?.groupValues?.get(1)
            ?: error("Cannot parse table from $sql")

    private fun tableNameFromSelect(sql: String): String =
        Regex("""FROM\s+(\w+)""", RegexOption.IGNORE_CASE)
            .find(sql)?.groupValues?.get(1)
            ?: error("Cannot parse table from $sql")
}
