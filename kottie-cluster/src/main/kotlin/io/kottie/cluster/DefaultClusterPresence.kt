package io.kottie.cluster

import io.kottie.api.cluster.ClusterPresenceService
import io.kottie.api.cluster.ClusterView
import io.kottie.api.cluster.InstanceId
import io.kottie.api.cluster.InstancePresence
import io.kottie.api.cluster.ServerPlatform
import io.kottie.api.network.NetworkBridge
import io.kottie.api.network.NetworkMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Heartbeat-based presence over a [NetworkBridge] channel.
 *
 * Wire format (v1 text): instanceId|platform|startedAtEpochMs|metaJsonSimple
 * Meta is optional key=value pairs separated by commas.
 */
class DefaultClusterPresenceService(
    private val bridge: NetworkBridge,
    private val scope: CoroutineScope,
    override val localInstanceId: InstanceId = InstanceId(),
    private val platform: ServerPlatform = ServerPlatform.PAPER,
    private val heartbeatInterval: Duration = Duration.ofSeconds(2),
    private val staleTimeout: Duration = Duration.ofSeconds(8),
    private val channel: String = "kottie:cluster:presence",
    private val metadata: Map<String, String> = emptyMap()
) : ClusterPresenceService, ClusterView {

    private val log = LoggerFactory.getLogger(DefaultClusterPresenceService::class.java)
    private val members = ConcurrentHashMap<String, InstancePresence>()
    private val started = AtomicBoolean(false)
    private val startedAt = Instant.now()
    private var beatJob: Job? = null
    private var sub: AutoCloseable? = null
    private var pruneJob: Job? = null

    override val view: ClusterView get() = this

    override fun members(): Collection<InstancePresence> = members.values.toList()

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        sub = bridge.subscribe(channel) { msg ->
            handlePresencePayload(msg.payload)
        }
        beatJob = scope.launch {
            while (isActive && started.get()) {
                runCatching { beat() }
                delay(heartbeatInterval.toMillis())
            }
        }
        pruneJob = scope.launch {
            while (isActive && started.get()) {
                pruneStale()
                delay(heartbeatInterval.toMillis())
            }
        }
        log.info("Cluster presence started id={}", localInstanceId.value)
    }

    override fun stop() {
        if (!started.compareAndSet(true, false)) return
        beatJob?.cancel()
        pruneJob?.cancel()
        sub?.close()
        sub = null
        members.clear()
        log.info("Cluster presence stopped id={}", localInstanceId.value)
    }

    override suspend fun beat() {
        val now = Instant.now()
        val local = InstancePresence(
            instanceId = localInstanceId,
            platform = platform,
            startedAt = startedAt,
            lastHeartbeat = now,
            metadata = metadata
        )
        members[localInstanceId.value] = local
        val payload = encode(local)
        bridge.publish(
            NetworkMessage(
                channel = channel,
                payload = payload,
                idempotencyKey = "presence-${localInstanceId.value}-${now.toEpochMilli()}",
                schemaId = "kottie:cluster-presence",
                schemaVersion = 1,
                sentAtEpochMs = now.toEpochMilli()
            )
        )
    }

    private fun handlePresencePayload(bytes: ByteArray) {
        val presence = decode(bytes) ?: return
        members[presence.instanceId.value] = presence
    }

    private fun pruneStale() {
        val now = Instant.now()
        members.entries.removeIf { (id, p) ->
            id != localInstanceId.value && p.isStale(now, staleTimeout)
        }
    }

    companion object {
        fun encode(p: InstancePresence): ByteArray {
            val meta = p.metadata.entries.joinToString(",") { "${it.key}=${it.value}" }
            val line = listOf(
                p.instanceId.value,
                p.platform.name,
                p.startedAt.toEpochMilli().toString(),
                p.lastHeartbeat.toEpochMilli().toString(),
                meta
            ).joinToString("|")
            return line.toByteArray(StandardCharsets.UTF_8)
        }

        fun decode(bytes: ByteArray): InstancePresence? {
            return try {
                val parts = bytes.toString(StandardCharsets.UTF_8).split("|")
                if (parts.size < 4) return null
                val meta = if (parts.size > 4 && parts[4].isNotBlank()) {
                    parts[4].split(",").mapNotNull {
                        val kv = it.split("=", limit = 2)
                        if (kv.size == 2) kv[0] to kv[1] else null
                    }.toMap()
                } else emptyMap()
                InstancePresence(
                    instanceId = InstanceId(parts[0]),
                    platform = runCatching { ServerPlatform.valueOf(parts[1]) }.getOrDefault(ServerPlatform.UNKNOWN),
                    startedAt = Instant.ofEpochMilli(parts[2].toLong()),
                    lastHeartbeat = Instant.ofEpochMilli(parts[3].toLong()),
                    metadata = meta
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}
