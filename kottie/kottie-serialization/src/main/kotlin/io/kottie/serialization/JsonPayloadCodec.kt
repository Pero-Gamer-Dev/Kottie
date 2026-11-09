package io.kottie.serialization

import io.kottie.api.serialization.PayloadCodec
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Reference [PayloadCodec] backed by kotlinx.serialization JSON.
 * Target type must be @Serializable.
 */
class JsonPayloadCodec<T : Any>(
    override val schemaId: String,
    override val schemaVersion: Int,
    private val serializer: KSerializer<T>,
    override val maxPayloadBytes: Int = PayloadCodec.DEFAULT_MAX_PAYLOAD_BYTES,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : PayloadCodec<T> {

    override fun encode(value: T): ByteArray =
        json.encodeToString(serializer, value).encodeToByteArray()

    override fun decode(bytes: ByteArray): T =
        json.decodeFromString(serializer, bytes.decodeToString())
}

/**
 * Factory using reified type + reflective serializer lookup.
 */
inline fun <reified T : Any> jsonPayloadCodec(
    schemaId: String,
    schemaVersion: Int,
    maxPayloadBytes: Int = PayloadCodec.DEFAULT_MAX_PAYLOAD_BYTES,
    json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
): PayloadCodec<T> = JsonPayloadCodec(
    schemaId = schemaId,
    schemaVersion = schemaVersion,
    serializer = serializer(),
    maxPayloadBytes = maxPayloadBytes,
    json = json
)
