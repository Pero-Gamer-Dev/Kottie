package io.kottie.network

import io.kottie.api.network.IdempotencyTracker
import io.kottie.api.network.MessageHandler
import io.kottie.api.network.NetworkBridge
import io.kottie.api.network.NetworkMessage
import io.kottie.api.network.NetworkState
import io.kottie.api.network.OutagePolicy
import io.kottie.api.network.ReconnectPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Redis-shaped [NetworkBridge] with reconnect/backoff, idempotency, and stale policy.
 *
 * Plug any [RedisTransport] (Lettuce-backed in production, [InMemoryRedisTransport] in tests).
 */
class RedisNetworkBridge(
    private val transport: RedisTransport,
    private val scope: CoroutineScope,
    override val reconnectPolicy: ReconnectPolicy = ReconnectPolicy(),
    override val outagePolicy: OutagePolicy = OutagePolicy.DROP_STALE,
    private val autoReconnect: Boolean = true
) : NetworkBridge {

    private val log = LoggerFactory.getLogger(RedisNetworkBridge::class.java)
    private val stateRef = AtomicReference(NetworkState.DISCONNECTED)
    override val state: NetworkState get() = stateRef.get()

    private val handlers = ConcurrentHashMap<String, CopyOnWriteArrayList<MessageHandler>>()
    private val transportSubs = ConcurrentHashMap<String, AutoCloseable>()
    private val dedup = IdempotencyTracker()
    private val lastAppliedEpoch = ConcurrentHashMap<String, Long>()
    private val mutex = Mutex()
    private val reconnectAttempt = AtomicInteger(0)
    private var reconnectJob: Job? = null

    override suspend fun connect() {
        mutex.withLock {
            if (stateRef.get() == NetworkState.CONNECTED && transport.isConnected) return
            stateRef.set(NetworkState.CONNECTING)
            try {
                transport.connect()
                resubscribeAll()
                reconnectAttempt.set(0)
                stateRef.set(NetworkState.CONNECTED)
                log.info("RedisNetworkBridge connected")
            } catch (t: Throwable) {
                stateRef.set(NetworkState.FAILED)
                log.error("Connect failed", t)
                if (autoReconnect) scheduleReconnect()
                throw t
            }
        }
    }

    override suspend fun disconnect() {
        mutex.withLock {
            reconnectJob?.cancel()
            reconnectJob = null
            transportSubs.values.forEach { runCatching { it.close() } }
            transportSubs.clear()
            runCatching { transport.disconnect() }
            stateRef.set(NetworkState.DISCONNECTED)
            log.info("RedisNetworkBridge disconnected")
        }
    }

    override suspend fun publish(message: NetworkMessage) {
        ensureConnected()
        val bytes = NetworkMessageCodec.encode(message)
        try {
            transport.publish(message.channel, bytes)
        } catch (t: Throwable) {
            log.warn("Publish failed; marking disconnected", t)
            stateRef.set(NetworkState.DISCONNECTED)
            if (autoReconnect) scheduleReconnect()
            throw t
        }
    }

    override fun subscribe(channel: String, handler: MessageHandler): AutoCloseable {
        val list = handlers.computeIfAbsent(channel) { CopyOnWriteArrayList() }
        list.add(handler)
        // Ensure transport subscription exists if connected
        scope.launch {
            mutex.withLock {
                if (stateRef.get() == NetworkState.CONNECTED) {
                    ensureTransportSubscription(channel)
                }
            }
        }
        return AutoCloseable {
            list.remove(handler)
            if (list.isEmpty()) {
                transportSubs.remove(channel)?.close()
            }
        }
    }

    /**
     * Notify the bridge that the underlying connection dropped (e.g. from a
     * Lettuce connection listener). Triggers reconnect when [autoReconnect] is true.
     */
    fun notifyConnectionLost() {
        val prev = stateRef.getAndSet(NetworkState.DISCONNECTED)
        if (prev == NetworkState.CONNECTED || prev == NetworkState.RECONNECTING) {
            log.warn("Connection lost")
            if (autoReconnect) scheduleReconnect()
        }
    }

    private suspend fun ensureConnected() {
        if (stateRef.get() == NetworkState.CONNECTED && transport.isConnected) return
        connect()
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            var attempt = reconnectAttempt.get()
            while (isActive && stateRef.get() != NetworkState.CONNECTED) {
                if (attempt >= reconnectPolicy.maxAttempts) {
                    stateRef.set(NetworkState.FAILED)
                    log.error("Max reconnect attempts reached ({})", attempt)
                    return@launch
                }
                stateRef.set(NetworkState.RECONNECTING)
                val delayMs = reconnectPolicy.delayForAttempt(attempt).toMillis()
                log.info("Reconnecting in {}ms (attempt {})", delayMs, attempt + 1)
                delay(delayMs)
                try {
                    connect()
                    return@launch
                } catch (_: Throwable) {
                    attempt = reconnectAttempt.incrementAndGet()
                }
            }
        }
    }

    private fun resubscribeAll() {
        handlers.keys.forEach { ensureTransportSubscription(it) }
    }

    private fun ensureTransportSubscription(channel: String) {
        if (transportSubs.containsKey(channel)) return
        val handle = transport.subscribe(channel) { bytes ->
            scope.launch {
                try {
                    val msg = NetworkMessageCodec.decode(bytes)
                    dispatchLocal(msg)
                } catch (t: Throwable) {
                    log.error("Failed to decode/dispatch message on {}", channel, t)
                }
            }
        }
        transportSubs[channel] = handle
    }

    private suspend fun dispatchLocal(message: NetworkMessage) {
        if (dedup.isDuplicate(message.idempotencyKey)) {
            log.debug("Duplicate idempotencyKey={}", message.idempotencyKey)
            return
        }
        when (outagePolicy) {
            OutagePolicy.DROP_STALE -> {
                val last = lastAppliedEpoch[message.channel] ?: 0L
                if (message.sentAtEpochMs < last) return
                lastAppliedEpoch[message.channel] = message.sentAtEpochMs
            }
            OutagePolicy.LAST_WRITE_WINS -> {
                lastAppliedEpoch.merge(message.channel, message.sentAtEpochMs) { a, b -> maxOf(a, b) }
            }
            OutagePolicy.DELIVER_ALL -> Unit
        }
        val list = handlers[message.channel] ?: return
        for (h in list.toList()) {
            try {
                h.onMessage(message)
            } catch (t: Throwable) {
                log.error("Handler failed on channel={}", message.channel, t)
            }
        }
    }
}
