package io.kottie.testkit

import io.kottie.api.serialization.PayloadCodec
import io.kottie.storage.FileSystemRepository
import io.kottie.storage.InMemorySqlDriver
import io.kottie.storage.SqlKeyValueRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

data class SavedProfile(val name: String, val level: Int)

class StringProfileCodec : PayloadCodec<SavedProfile> {
    override val schemaId: String = "test:profile"
    override val schemaVersion: Int = 1
    override val maxPayloadBytes: Int = 4096

    override fun encode(value: SavedProfile): ByteArray =
        "${value.name}\n${value.level}".encodeToByteArray()

    override fun decode(bytes: ByteArray): SavedProfile {
        val lines = bytes.decodeToString().split("\n")
        require(lines.size == 2)
        return SavedProfile(lines[0], lines[1].toInt())
    }
}

class PersistentStorageTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `file repository survives new instance`() = runBlocking {
        val codec = StringProfileCodec()
        val dir = tempDir.resolve("profiles")
        val repo1 = FileSystemRepository(dir, codec)
        repo1.save("steve", SavedProfile("Steve", 10))
        assertTrue(repo1.exists("steve"))

        // "restart"
        val repo2 = FileSystemRepository(dir, codec)
        val loaded = repo2.find("steve")
        assertNotNull(loaded)
        assertEquals("Steve", loaded!!.name)
        assertEquals(10, loaded.level)
    }

    @Test
    fun `file repository delete`() = runBlocking {
        val codec = StringProfileCodec()
        val repo = FileSystemRepository(tempDir.resolve("del"), codec)
        repo.save("a", SavedProfile("A", 1))
        assertTrue(repo.delete("a"))
        assertNull(repo.find("a"))
        assertFalse(repo.exists("a"))
    }

    @Test
    fun `file repository rejects path traversal style keys via hashing`() = runBlocking {
        val codec = StringProfileCodec()
        val repo = FileSystemRepository(tempDir.resolve("safe"), codec)
        repo.save("../etc/passwd", SavedProfile("x", 1))
        // Must not create files outside root — only hashed name under root
        val files = tempDir.resolve("safe").toFile().listFiles()?.map { it.name } ?: emptyList()
        assertTrue(files.all { it.endsWith(".bin") || it.endsWith(".tmp") })
        assertEquals("x", repo.find("../etc/passwd")?.name)
    }

    @Test
    fun `sql key value repository with in-memory driver`() = runBlocking {
        val driver = InMemorySqlDriver()
        driver.connect()
        val codec = StringProfileCodec()
        val repo = SqlKeyValueRepository(driver, codec)
        repo.ensureSchema()
        repo.save("alex", SavedProfile("Alex", 42))
        assertEquals(42, repo.find("alex")?.level)
        assertTrue(repo.exists("alex"))
        assertTrue(repo.delete("alex"))
        assertNull(repo.find("alex"))
        driver.close()
    }

    @Test
    fun `oversized encode rejected`() = runBlocking {
        val tiny = object : PayloadCodec<SavedProfile> {
            override val schemaId = "t"
            override val schemaVersion = 1
            override val maxPayloadBytes = 4
            override fun encode(value: SavedProfile) = "too-large-payload".encodeToByteArray()
            override fun decode(bytes: ByteArray) = SavedProfile("z", 0)
        }
        val repo = FileSystemRepository(tempDir.resolve("tiny"), tiny)
        assertThrows(IllegalStateException::class.java) {
            runBlocking { repo.save("k", SavedProfile("z", 0)) }
        }
    }
}
