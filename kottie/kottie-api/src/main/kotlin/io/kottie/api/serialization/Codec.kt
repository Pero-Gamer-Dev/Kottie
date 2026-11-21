package io.kottie.api.serialization

/**
 * Transport-neutral codec for a domain type.
 *
 * Requirements:
 * - Stable [schemaId] + [schemaVersion] for evolution and migrations
 * - Encode/decode operate on byte arrays (no live server object graphs)
 * - Callers should enforce [maxPayloadBytes] before decode/use
 */
interface PayloadCodec<T : Any> {
    val schemaId: String
    val schemaVersion: Int
    val maxPayloadBytes: Int
        get() = DEFAULT_MAX_PAYLOAD_BYTES

    fun encode(value: T): ByteArray
    fun decode(bytes: ByteArray): T

    companion object {
        const val DEFAULT_MAX_PAYLOAD_BYTES: Int = 1_048_576 // 1 MiB
    }
}

/**
 * Result of a guarded decode that enforces size limits and reports errors.
 */
sealed class DecodeResult<out T> {
    data class Ok<T>(val value: T) : DecodeResult<T>()
    data class Failed(
        val reason: Reason,
        val message: String,
        val cause: Throwable? = null
    ) : DecodeResult<Nothing>()

    enum class Reason {
        PAYLOAD_TOO_LARGE,
        MALFORMED,
        SCHEMA_MISMATCH,
        VALIDATION
    }
}

/**
 * Helper that rejects oversized payloads before calling [PayloadCodec.decode].
 */
fun <T : Any> PayloadCodec<T>.safeDecode(bytes: ByteArray): DecodeResult<T> {
    if (bytes.size > maxPayloadBytes) {
        return DecodeResult.Failed(
            DecodeResult.Reason.PAYLOAD_TOO_LARGE,
            "Payload size ${bytes.size} exceeds limit $maxPayloadBytes for schema $schemaId@$schemaVersion"
        )
    }
    return try {
        DecodeResult.Ok(decode(bytes))
    } catch (t: Throwable) {
        DecodeResult.Failed(
            DecodeResult.Reason.MALFORMED,
            t.message ?: "decode failed",
            t
        )
    }
}

/**
 * Optional migration from an older schema version to the current codec version.
 */
interface SchemaMigration<T : Any> {
    val fromVersion: Int
    val toVersion: Int
    fun migrate(bytes: ByteArray): ByteArray
}
