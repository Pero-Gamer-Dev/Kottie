package io.kottie.network

/**
 * Host-side wiring notes for Lettuce (not a compile dependency of Kottie).
 *
 * ```kotlin
 * // In your server/proxy plugin (compileOnly/implementation io.lettuce:lettuce-core)
 * class LettuceRedisTransport(
 *     private val client: io.lettuce.core.RedisClient,
 *     private val uri: String
 * ) : RedisTransport {
 *     private var conn: io.lettuce.core.pubsub.StatefulRedisPubSubConnection<String, ByteArray>? = null
 *     // map ByteArray codecs via RedisCodec.of(...)
 *     override val isConnected get() = conn?.isOpen == true
 *     override suspend fun connect() { /* client.connectPubSub(codec, redisURI) */ }
 *     override suspend fun disconnect() { conn?.close() }
 *     override suspend fun publish(channel: String, payload: ByteArray) {
 *         conn!!.async().publish(channel, payload)
 *     }
 *     override fun subscribe(channel: String, handler: (ByteArray) -> Unit): AutoCloseable {
 *         // addListener + async().subscribe(channel)
 *         return AutoCloseable { /* unsubscribe */ }
 *     }
 * }
 *
 * val transport = ReconnectableRedisTransport(
 *     NamespacedRedisTransport(LettuceRedisTransport(client, uri), "prod"),
 *     scope
 * )
 * // On Lettuce connection listener: transport.notifyDisconnected()
 * ```
 */
object LettuceWiringDocs
