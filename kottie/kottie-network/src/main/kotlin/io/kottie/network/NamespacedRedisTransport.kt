package io.kottie.network

/**
 * Prefixes every channel with [namespace] (e.g. `prod:` or `kottie:eu:`).
 * Useful when multiple environments share one Redis.
 */
class NamespacedRedisTransport(
    private val delegate: RedisTransport,
    namespace: String
) : RedisTransport {

    private val prefix: String = when {
        namespace.isBlank() -> ""
        namespace.endsWith(":") -> namespace
        else -> "$namespace:"
    }

    override val isConnected: Boolean
        get() = delegate.isConnected

    override suspend fun connect() = delegate.connect()

    override suspend fun disconnect() = delegate.disconnect()

    override suspend fun publish(channel: String, payload: ByteArray) {
        delegate.publish(prefix + channel, payload)
    }

    override fun subscribe(channel: String, handler: (ByteArray) -> Unit): AutoCloseable =
        delegate.subscribe(prefix + channel, handler)

    fun physicalChannel(logical: String): String = prefix + logical
}
