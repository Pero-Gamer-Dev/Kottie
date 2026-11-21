package io.kottie.api.cluster

import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Stable identity for one running server process in a multi-instance deployment.
 */
data class InstanceId(
    val value: String = UUID.randomUUID().toString()
) {
    init {
        require(value.isNotBlank()) { "InstanceId must not be blank" }
    }
}

/**
 * Declared server platform family. Used for documentation and adapter selection,
 * not as a hard runtime dependency on a specific JAR.
 */
enum class ServerPlatform {
    PAPER,
    FOLIA,
    PURPUR,
    SPIGOT,
    UNKNOWN
}

/**
 * Presence record advertised by an instance over the network bridge.
 */
data class InstancePresence(
    val instanceId: InstanceId,
    val platform: ServerPlatform,
    val startedAt: Instant,
    val lastHeartbeat: Instant,
    val metadata: Map<String, String> = emptyMap()
) {
    fun isStale(now: Instant, timeout: Duration): Boolean =
        Duration.between(lastHeartbeat, now) > timeout
}

/**
 * Cluster-wide view of live instances.
 */
interface ClusterView {
    val localInstanceId: InstanceId
    fun members(): Collection<InstancePresence>
    fun isLocal(id: InstanceId): Boolean = id == localInstanceId
}

/**
 * Publishes heartbeats and tracks peer presence.
 */
interface ClusterPresenceService {
    val view: ClusterView

    fun start()
    fun stop()
    suspend fun beat()
}

data class ClusterEnvelope(
    val from: InstanceId,
    val topic: String,
    val payload: ByteArray,
    val correlationId: String = UUID.randomUUID().toString(),
    val sentAtEpochMs: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClusterEnvelope) return false
        return from == other.from && topic == other.topic &&
            correlationId == other.correlationId && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var r = from.hashCode()
        r = 31 * r + topic.hashCode()
        r = 31 * r + correlationId.hashCode()
        r = 31 * r + payload.contentHashCode()
        return r
    }
}

fun interface ClusterMessageHandler {
    suspend fun onMessage(envelope: ClusterEnvelope)
}

interface ClusterMessenger {
    val localInstanceId: InstanceId
    suspend fun publish(topic: String, payload: ByteArray)
    fun subscribe(topic: String, handler: ClusterMessageHandler): AutoCloseable
}
