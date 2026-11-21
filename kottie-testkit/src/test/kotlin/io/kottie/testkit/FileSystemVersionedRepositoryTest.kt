package io.kottie.testkit

import io.kottie.storage.FileSystemVersionedRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class FileSystemVersionedRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `versioned save and conflict`() = runBlocking {
        val codec = StringProfileCodec()
        val repo = FileSystemVersionedRepository(tempDir.resolve("v"), codec)

        assertTrue(repo.saveIfVersion("p1", SavedProfile("A", 1), expectedVersion = 0L))
        val v1 = repo.findVersioned("p1")!!
        assertEquals(1L, v1.version)
        assertEquals("A", v1.value.name)

        assertFalse(repo.saveIfVersion("p1", SavedProfile("B", 2), expectedVersion = 0L))
        assertEquals("A", repo.find("p1")?.name)

        assertTrue(repo.saveIfVersion("p1", SavedProfile("B", 2), expectedVersion = 1L))
        assertEquals(2L, repo.findVersioned("p1")!!.version)
        assertEquals("B", repo.find("p1")?.name)
    }

    @Test
    fun `survives new instance`() = runBlocking {
        val codec = StringProfileCodec()
        val dir = tempDir.resolve("persist")
        val repo1 = FileSystemVersionedRepository(dir, codec)
        repo1.save("x", SavedProfile("X", 9))
        val repo2 = FileSystemVersionedRepository(dir, codec)
        assertEquals(9, repo2.find("x")?.level)
        assertTrue(repo2.findVersioned("x")!!.version >= 1L)
    }
}
