package io.kottie.network

/**
 * Minimal pub/sub transport used by [RedisNetworkBridge].
 *
 * Production hosts implement this with Lettuce, Jedis, or another client.
 * Tests use [InMemoryRedisTransport]. No Redis library is required at compile time.
 */
interface RedisTransport {
    val isConnected: Boolean

    /** Open the underlying connection. Idempotent if already connected. */
    suspend fun connect()

    /** Close the connection and release resources. */
    suspend fun disconnect()

    /**
     * Publish raw bytes to a Redis channel (or equivalent).
     * Throws if not connected.
     */
    suspend fun publish(channel: String, payload: ByteArray)

    /**
     * Subscribe to a channel. [handler] may be invoked on a transport thread;
     * the bridge is responsible for dispatching to coroutines.
     * Returns a handle that unsubscribes on close.
     */
    fun subscribe(channel: String, handler: (ByteArray) -> Unit): AutoCloseable
}

/**
 * Process-local transport for tests and single-node demos.
 */
class InMemoryRedisTransport : RedisTransport {
    private val lock = Any()
    private var connected = false
    private val subs = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<(ByteArray) -> Unit>>()

    override val isConnected: Boolean
        get() = synchronized(lock) { connected }

    override suspend fun connect() {
        synchronized(lock) { connected = true }
    }

    override suspend fun disconnect() {
        synchronized(lock) { connected = false }
    }

    override suspend fun publish(channel: String, payload: ByteArray) {
        check(isConnected) { "transport not connected" }
        val handlers = subs[channel]?.toList().orEmpty()
        handlers.forEach { h ->
            try {
                h(payload)
            } catch (_: Throwable) {
            }
        }
    }

    override fun subscribe(channel: String, handler: (ByteArray) -> Unit): AutoCloseable {
        val list = subs.computeIfAbsent(channel) { java.util.concurrent.CopyOnWriteArrayList() }
        list.add(handler)
        return AutoCloseable { list.remove(handler) }
    }

    /** Test helper: force disconnect without clearing subscriptions. */
    fun simulateDrop() {
        synchronized(lock) { connected = false }
    }
}
