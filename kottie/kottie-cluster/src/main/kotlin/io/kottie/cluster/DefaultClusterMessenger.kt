package io.kottie.cluster

import io.kottie.api.cluster.ClusterEnvelope
import io.kottie.api.cluster.ClusterMessageHandler
import io.kottie.api.cluster.ClusterMessenger
import io.kottie.api.cluster.InstanceId
import io.kottie.api.network.NetworkBridge
import io.kottie.api.network.NetworkMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Topic-based cluster messaging on top of [NetworkBridge].
 * Channel layout: `kottie:cluster:msg:{topic}`
 */
class DefaultClusterMessenger(
    private val bridge: NetworkBridge,
    private val scope: CoroutineScope,
    override val localInstanceId: InstanceId = InstanceId()
) : ClusterMessenger {

    private val log = LoggerFactory.getLogger(DefaultClusterMessenger::class.java)
    private val handlers = ConcurrentHashMap<String, CopyOnWriteArrayList<ClusterMessageHandler>>()
    private val transportSubs = ConcurrentHashMap<String, AutoCloseable>()

    override suspend fun publish(topic: String, payload: ByteArray) {
        val envelope = ClusterEnvelope(
            from = localInstanceId,
            topic = topic,
            payload = payload
        )
        bridge.publish(
            NetworkMessage(
                channel = channelFor(topic),
                payload = encode(envelope),
                idempotencyKey = envelope.correlationId,
                schemaId = "kottie:cluster-envelope",
                schemaVersion = 1,
                sentAtEpochMs = envelope.sentAtEpochMs
            )
        )
    }

    override fun subscribe(topic: String, handler: ClusterMessageHandler): AutoCloseable {
        val list = handlers.computeIfAbsent(topic) { CopyOnWriteArrayList() }
        list.add(handler)
        transportSubs.computeIfAbsent(topic) {
            bridge.subscribe(channelFor(topic)) { msg ->
                scope.launch {
                    val env = decode(msg.payload) ?: return@launch
                    // Optional: skip messages from self
                    dispatch(topic, env)
                }
            }
        }
        return AutoCloseable {
            list.remove(handler)
            if (list.isEmpty()) {
                transportSubs.remove(topic)?.close()
            }
        }
    }

    private fun dispatch(topic: String, envelope: ClusterEnvelope) {
        val list = handlers[topic] ?: return
        for (h in list.toList()) {
            scope.launch {
                try {
                    h.onMessage(envelope)
                } catch (t: Throwable) {
                    log.error("Cluster handler failed topic={}", topic, t)
                }
            }
        }
    }

    companion object {
        fun channelFor(topic: String): String = "kottie:cluster:msg:$topic"

        fun encode(env: ClusterEnvelope): ByteArray {
            val from = env.from.value.toByteArray(StandardCharsets.UTF_8)
            val topic = env.topic.toByteArray(StandardCharsets.UTF_8)
            val corr = env.correlationId.toByteArray(StandardCharsets.UTF_8)
            val size = 2 + from.size + 2 + topic.size + 2 + corr.size + 8 + 4 + env.payload.size
            val buf = ByteBuffer.allocate(size)
            fun put(b: ByteArray) {
                buf.putShort(b.size.toShort())
                buf.put(b)
            }
            put(from)
            put(topic)
            put(corr)
            buf.putLong(env.sentAtEpochMs)
            put(env.payload)
            return buf.array()
        }

        fun decode(bytes: ByteArray): ClusterEnvelope? {
            return try {
                val buf = ByteBuffer.wrap(bytes)
                fun get(): ByteArray {
                    val n = buf.short.toInt() and 0xFFFF
                    val a = ByteArray(n)
                    buf.get(a)
                    return a
                }
                val from = get().toString(StandardCharsets.UTF_8)
                val topic = get().toString(StandardCharsets.UTF_8)
                val corr = get().toString(StandardCharsets.UTF_8)
                val sent = buf.long
                val payload = get()
                ClusterEnvelope(
                    from = InstanceId(from),
                    topic = topic,
                    payload = payload,
                    correlationId = corr,
                    sentAtEpochMs = sent
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}
