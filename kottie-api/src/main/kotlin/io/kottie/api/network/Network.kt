package io.kottie.api.network

import java.time.Duration
import java.util.UUID

/**
 * Transport-neutral messaging envelope.
 * Payload must be a domain snapshot (never a live server object).
 */
data class NetworkMessage(
    val channel: String,
    val payload: ByteArray,
    /** Stable id for at-least-once / dedup. */
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val schemaId: String = "",
    val schemaVersion: Int = 0,
    val sentAtEpochMs: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NetworkMessage) return false
        return channel == other.channel &&
            idempotencyKey == other.idempotencyKey &&
            schemaId == other.schemaId &&
            schemaVersion == other.schemaVersion &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = channel.hashCode()
        result = 31 * result + idempotencyKey.hashCode()
        result = 31 * result + schemaId.hashCode()
        result = 31 * result + schemaVersion
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

/**
 * Connection lifecycle for a network bridge.
 */
enum class NetworkState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

/**
 * Backoff policy for reconnect attempts.
 */
data class ReconnectPolicy(
    val initialDelay: Duration = Duration.ofMillis(200),
    val maxDelay: Duration = Duration.ofSeconds(30),
    val multiplier: Double = 2.0,
    val maxAttempts: Int = Int.MAX_VALUE
) {
    fun delayForAttempt(attempt: Int): Duration {
        if (attempt <= 0) return initialDelay
        var ms = initialDelay.toMillis().toDouble()
        repeat(attempt) { ms *= multiplier }
        return Duration.ofMillis(ms.toLong().coerceAtMost(maxDelay.toMillis()))
    }
}

/**
 * How to treat concurrent or out-of-order updates for the same logical key.
 */
enum class OutagePolicy {
    /** Prefer the message with the latest sentAtEpochMs. */
    LAST_WRITE_WINS,
    /** Drop messages older than a watermark already applied. */
    DROP_STALE,
    /** Deliver everything; application decides. */
    DELIVER_ALL
}

/**
 * Handler for inbound messages on a channel.
 * Must be non-blocking / offload heavy work.
 */
fun interface MessageHandler {
    suspend fun onMessage(message: NetworkMessage)
}

/**
 * Optional multi-instance messaging bridge (Redis, NATS, etc. in production).
 * Core runtime does not require a network bridge to boot.
 */
interface NetworkBridge {
    val state: NetworkState
    val reconnectPolicy: ReconnectPolicy
    val outagePolicy: OutagePolicy

    suspend fun connect()
    suspend fun disconnect()

    /**
     * Publish a message. Implementations should surface transport failures
     * as exceptions; callers may retry with the same [NetworkMessage.idempotencyKey].
     */
    suspend fun publish(message: NetworkMessage)

    /**
     * Subscribe to a channel. Duplicate idempotency keys may be filtered
     * according to implementation policy.
     */
    fun subscribe(channel: String, handler: MessageHandler): AutoCloseable
}

/**
 * Simple dedup window for idempotency keys.
 */
class IdempotencyTracker(
    private val capacity: Int = 10_000
) {
    private val seen = LinkedHashSet<String>()

    @Synchronized
    fun isDuplicate(key: String): Boolean {
        if (seen.contains(key)) return true
        seen.add(key)
        while (seen.size > capacity) {
            val first = seen.iterator().next()
            seen.remove(first)
        }
        return false
    }

    @Synchronized
    fun clear() = seen.clear()
}
