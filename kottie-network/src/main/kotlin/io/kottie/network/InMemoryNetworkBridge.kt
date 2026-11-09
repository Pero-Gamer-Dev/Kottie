package io.kottie.network

import io.kottie.api.network.IdempotencyTracker
import io.kottie.api.network.MessageHandler
import io.kottie.api.network.NetworkBridge
import io.kottie.api.network.NetworkMessage
import io.kottie.api.network.NetworkState
import io.kottie.api.network.OutagePolicy
import io.kottie.api.network.ReconnectPolicy
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Single-process network bridge for tests and local multi-plugin demos.
 * Simulates connect/disconnect, idempotent delivery, and stale-drop policy.
 * Not a substitute for Redis/NATS in production.
 */
class InMemoryNetworkBridge(
    override val reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
    override val outagePolicy: OutagePolicy = OutagePolicy.DROP_STALE
) : NetworkBridge {

    private val log = LoggerFactory.getLogger(InMemoryNetworkBridge::class.java)
    private val stateRef = AtomicReference(NetworkState.DISCONNECTED)
    override val state: NetworkState get() = stateRef.get()

    private val handlers = ConcurrentHashMap<String, CopyOnWriteArrayList<MessageHandler>>()
    private val dedup = IdempotencyTracker()
    private val lastAppliedEpoch = ConcurrentHashMap<String, Long>()
    private val mutex = Mutex()

    /** Test hook: force a disconnected state without clearing subscriptions. */
    fun simulateDisconnect() {
        stateRef.set(NetworkState.DISCONNECTED)
    }

    override suspend fun connect() {
        mutex.withLock {
            when (stateRef.get()) {
                NetworkState.CONNECTED -> return
                NetworkState.FAILED, NetworkState.DISCONNECTED, NetworkState.RECONNECTING, NetworkState.CONNECTING -> {
                    stateRef.set(NetworkState.CONNECTING)
                    stateRef.set(NetworkState.CONNECTED)
                    log.info("InMemoryNetworkBridge connected")
                }
            }
        }
    }

    override suspend fun disconnect() {
        mutex.withLock {
            stateRef.set(NetworkState.DISCONNECTED)
            log.info("InMemoryNetworkBridge disconnected")
        }
    }

    override suspend fun publish(message: NetworkMessage) {
        if (stateRef.get() != NetworkState.CONNECTED) {
            error("Cannot publish while state=${stateRef.get()}")
        }
        deliver(message)
    }

    /**
     * Inject a message as if received from the wire (used in tests for
     * duplicate / stale scenarios). Respects connection state.
     */
    suspend fun receive(message: NetworkMessage) {
        if (stateRef.get() != NetworkState.CONNECTED) {
            error("Cannot receive while state=${stateRef.get()}")
        }
        deliver(message)
    }

    private suspend fun deliver(message: NetworkMessage) {
        if (dedup.isDuplicate(message.idempotencyKey)) {
            log.debug("Dropping duplicate idempotencyKey={}", message.idempotencyKey)
            return
        }
        when (outagePolicy) {
            OutagePolicy.DROP_STALE -> {
                val last = lastAppliedEpoch[message.channel] ?: 0L
                if (message.sentAtEpochMs < last) {
                    log.debug("Dropping stale message on channel={}", message.channel)
                    return
                }
                lastAppliedEpoch[message.channel] = message.sentAtEpochMs
            }
            OutagePolicy.LAST_WRITE_WINS -> {
                lastAppliedEpoch.merge(message.channel, message.sentAtEpochMs) { a, b -> maxOf(a, b) }
            }
            OutagePolicy.DELIVER_ALL -> { /* no filter */ }
        }
        val list = handlers[message.channel] ?: return
        for (h in list.toList()) {
            try {
                h.onMessage(message)
            } catch (t: Throwable) {
                log.error("Network handler failed on channel={}", message.channel, t)
            }
        }
    }

    override fun subscribe(channel: String, handler: MessageHandler): AutoCloseable {
        val list = handlers.computeIfAbsent(channel) { CopyOnWriteArrayList() }
        list.add(handler)
        return AutoCloseable { list.remove(handler) }
    }
}
