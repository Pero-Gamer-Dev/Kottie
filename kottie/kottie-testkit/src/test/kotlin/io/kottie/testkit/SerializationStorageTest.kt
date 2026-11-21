package io.kottie.testkit

import io.kottie.api.serialization.DecodeResult
import io.kottie.api.serialization.PayloadCodec
import io.kottie.api.serialization.safeDecode
import io.kottie.storage.InMemoryRepository
import io.kottie.storage.InMemoryVersionedRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

data class PlayerProfile(
    val name: String,
    val level: Int
)

/** Manual codec (no kotlinx dependency required in the test type). */
class PlayerProfileCodec(
    override val maxPayloadBytes: Int = 256
) : PayloadCodec<PlayerProfile> {
    override val schemaId: String = "kottie:player-profile"
    override val schemaVersion: Int = 1

    override fun encode(value: PlayerProfile): ByteArray =
        "${value.name}\n${value.level}".encodeToByteArray()

    override fun decode(bytes: ByteArray): PlayerProfile {
        val text = bytes.decodeToString()
        val lines = text.split("\n")
        require(lines.size == 2) { "expected 2 lines" }
        return PlayerProfile(lines[0], lines[1].toInt())
    }
}

class SerializationStorageTest {

    @Test
    fun `codec round trip`() {
        val codec = PlayerProfileCodec()
        val original = PlayerProfile("Alex", 42)
        val bytes = codec.encode(original)
        val decoded = codec.decode(bytes)
        assertEquals(original, decoded)
        assertEquals("kottie:player-profile", codec.schemaId)
        assertEquals(1, codec.schemaVersion)
    }

    @Test
    fun `safeDecode rejects oversized payload`() {
        val codec = PlayerProfileCodec(maxPayloadBytes = 8)
        val big = ByteArray(64) { 'x'.code.toByte() }
        val result = codec.safeDecode(big)
        assertTrue(result is DecodeResult.Failed)
        assertEquals(DecodeResult.Reason.PAYLOAD_TOO_LARGE, (result as DecodeResult.Failed).reason)
    }

    @Test
    fun `safeDecode reports malformed payload`() {
        val codec = PlayerProfileCodec()
        val result = codec.safeDecode("not-valid".encodeToByteArray())
        assertTrue(result is DecodeResult.Failed)
        assertEquals(DecodeResult.Reason.MALFORMED, (result as DecodeResult.Failed).reason)
    }

    @Test
    fun `safeDecode success`() {
        val codec = PlayerProfileCodec()
        val bytes = codec.encode(PlayerProfile("Steve", 1))
        val result = codec.safeDecode(bytes)
        assertTrue(result is DecodeResult.Ok)
        assertEquals("Steve", (result as DecodeResult.Ok).value.name)
    }

    @Test
    fun `in-memory repository crud`() = runBlocking {
        val repo = InMemoryRepository<String, PlayerProfile>()
        assertNull(repo.find("p1"))
        repo.save("p1", PlayerProfile("Alex", 10))
        assertEquals("Alex", repo.find("p1")?.name)
        assertTrue(repo.exists("p1"))
        assertTrue(repo.delete("p1"))
        assertFalse(repo.exists("p1"))
    }

    @Test
    fun `versioned repository conflict detection`() = runBlocking {
        val repo = InMemoryVersionedRepository<String, PlayerProfile>()
        assertTrue(repo.saveIfVersion("p1", PlayerProfile("A", 1), expectedVersion = 0L))
        val v1 = repo.findVersioned("p1")!!
        assertEquals(1L, v1.version)

        // stale write fails
        assertFalse(repo.saveIfVersion("p1", PlayerProfile("B", 2), expectedVersion = 0L))
        assertEquals("A", repo.find("p1")?.name)

        // correct version succeeds
        assertTrue(repo.saveIfVersion("p1", PlayerProfile("B", 2), expectedVersion = 1L))
        assertEquals("B", repo.find("p1")?.name)
        assertEquals(2L, repo.findVersioned("p1")!!.version)
    }

    @Test
    fun `repository plus codec persistence round trip`() = runBlocking {
        val codec = PlayerProfileCodec()
        val repo = InMemoryRepository<String, ByteArray>()
        val profile = PlayerProfile("Notch", 99)
        repo.save("notch", codec.encode(profile))
        val loaded = codec.safeDecode(repo.find("notch")!!)
        assertTrue(loaded is DecodeResult.Ok)
        assertEquals(profile, (loaded as DecodeResult.Ok).value)
    }
}
