package io.kottie.network

import io.kottie.api.network.NetworkMessage
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Compact binary framing for [NetworkMessage] over Redis pub/sub.
 *
 * Layout (version 1):
 * - 1 byte frame version
 * - 2 bytes channel length + channel UTF-8
 * - 2 bytes idempotencyKey length + key UTF-8
 * - 2 bytes schemaId length + schemaId UTF-8
 * - 4 bytes schemaVersion
 * - 8 bytes sentAtEpochMs
 * - 4 bytes payload length + payload
 */
object NetworkMessageCodec {
    private const val FRAME_VERSION: Byte = 1

    fun encode(message: NetworkMessage): ByteArray {
        val channel = message.channel.toByteArray(StandardCharsets.UTF_8)
        val key = message.idempotencyKey.toByteArray(StandardCharsets.UTF_8)
        val schema = message.schemaId.toByteArray(StandardCharsets.UTF_8)
        val size = 1 + 2 + channel.size + 2 + key.size + 2 + schema.size + 4 + 8 + 4 + message.payload.size
        val buf = ByteBuffer.allocate(size)
        buf.put(FRAME_VERSION)
        putBytes(buf, channel)
        putBytes(buf, key)
        putBytes(buf, schema)
        buf.putInt(message.schemaVersion)
        buf.putLong(message.sentAtEpochMs)
        putBytes(buf, message.payload)
        return buf.array()
    }

    fun decode(bytes: ByteArray): NetworkMessage {
        val buf = ByteBuffer.wrap(bytes)
        val ver = buf.get()
        require(ver == FRAME_VERSION) { "unsupported frame version $ver" }
        val channel = getString(buf)
        val key = getString(buf)
        val schema = getString(buf)
        val schemaVersion = buf.int
        val sentAt = buf.long
        val payload = getBytes(buf)
        return NetworkMessage(
            channel = channel,
            payload = payload,
            idempotencyKey = key,
            schemaId = schema,
            schemaVersion = schemaVersion,
            sentAtEpochMs = sentAt
        )
    }

    private fun putBytes(buf: ByteBuffer, data: ByteArray) {
        require(data.size <= 0xFFFF) { "field too large" }
        buf.putShort(data.size.toShort())
        buf.put(data)
    }

    private fun getBytes(buf: ByteBuffer): ByteArray {
        val len = buf.short.toInt() and 0xFFFF
        val data = ByteArray(len)
        buf.get(data)
        return data
    }

    private fun getString(buf: ByteBuffer): String =
        getBytes(buf).toString(StandardCharsets.UTF_8)
}
