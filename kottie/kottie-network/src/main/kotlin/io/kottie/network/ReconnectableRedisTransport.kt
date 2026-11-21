package io.kottie.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Decorator that re-establishes [delegate] connectivity using exponential backoff.
 *
 * Call [notifyDisconnected] from your Lettuce/Jedis connection listener when the
 * underlying link drops. Active [subscribe] handlers are re-bound after reconnect.
 */
class ReconnectableRedisTransport(
    private val delegate: RedisTransport,
    private val scope: CoroutineScope,
    private val initialDelay: Duration = Duration.ofMillis(200),
    private val maxDelay: Duration = Duration.ofSeconds(30),
    private val multiplier: Double = 2.0,
    private val maxAttempts: Int = Int.MAX_VALUE
) : RedisTransport {

    private val log = LoggerFactory.getLogger(ReconnectableRedisTransport::class.java)
    private val mutex = Mutex()
    private val attempt = AtomicInteger(0)
    private val wantConnected = AtomicBoolean(false)
    private var reconnectJob: Job? = null

    private data class Sub(val channel: String, val handler: (ByteArray) -> Unit)
    private val logicalSubs = CopyOnWriteArrayList<Sub>()
    private val liveHandles = CopyOnWriteArrayList<AutoCloseable>()

    override val isConnected: Boolean
        get() = delegate.isConnected

    override suspend fun connect() {
        wantConnected.set(true)
        mutex.withLock {
            delegate.connect()
            rebindSubscriptions()
            attempt.set(0)
        }
    }

    override suspend fun disconnect() {
        wantConnected.set(false)
        reconnectJob?.cancel()
        reconnectJob = null
        mutex.withLock {
            liveHandles.forEach { runCatching { it.close() } }
            liveHandles.clear()
            runCatching { delegate.disconnect() }
        }
    }

    override suspend fun publish(channel: String, payload: ByteArray) {
        if (!delegate.isConnected) {
            connect()
        }
        delegate.publish(channel, payload)
    }

    override fun subscribe(channel: String, handler: (ByteArray) -> Unit): AutoCloseable {
        val sub = Sub(channel, handler)
        logicalSubs.add(sub)
        if (delegate.isConnected) {
            val handle = delegate.subscribe(channel, handler)
            liveHandles.add(handle)
            return AutoCloseable {
                logicalSubs.remove(sub)
                liveHandles.remove(handle)
                runCatching { handle.close() }
            }
        }
        return AutoCloseable { logicalSubs.remove(sub) }
    }

    /**
     * Host Redis client should call this on connection loss.
     */
    fun notifyDisconnected() {
        if (!wantConnected.get()) return
        log.warn("Redis transport disconnected; scheduling reconnect")
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            var n = attempt.get()
            while (isActive && wantConnected.get() && !delegate.isConnected) {
                if (n >= maxAttempts) {
                    log.error("Redis reconnect gave up after {} attempts", n)
                    return@launch
                }
                val delayMs = delayFor(n)
                log.info("Redis reconnect in {}ms (attempt {})", delayMs, n + 1)
                delay(delayMs)
                try {
                    mutex.withLock {
                        delegate.connect()
                        rebindSubscriptions()
                        attempt.set(0)
                    }
                    log.info("Redis reconnected")
                    return@launch
                } catch (t: Throwable) {
                    n = attempt.incrementAndGet()
                    log.warn("Redis reconnect failed: {}", t.message)
                }
            }
        }
    }

    private fun rebindSubscriptions() {
        liveHandles.forEach { runCatching { it.close() } }
        liveHandles.clear()
        for (sub in logicalSubs) {
            val handle = delegate.subscribe(sub.channel, sub.handler)
            liveHandles.add(handle)
        }
    }

    private fun delayFor(attempt: Int): Long {
        var ms = initialDelay.toMillis().toDouble()
        repeat(attempt.coerceAtLeast(0)) { ms *= multiplier }
        return ms.toLong().coerceAtMost(maxDelay.toMillis())
    }
}
