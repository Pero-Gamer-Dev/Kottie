package io.kottie.adapter.paper

import io.kottie.api.packet.PacketBus
import io.kottie.api.packet.PacketDirection
import io.kottie.api.packet.PacketType
import io.kottie.packets.InMemoryPacketBus
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Maps Bukkit/Paper **plugin messaging channels** into Kottie [PacketBus] events.
 *
 * No compile-time dependency on Bukkit. The host plugin should:
 * 1. Register incoming plugin channels with the server
 * 2. Forward received bytes via [onPluginMessageReceived]
 * 3. Use [OutboundSink] to send outgoing payloads back through Bukkit
 *
 * Example host wiring:
 * ```
 * val bus = InMemoryPacketBus(adapterName = "paper-plugin-channel")
 * val bridge = PluginChannelPacketBridge(bus)
 * // Messenger.registerIncomingPluginChannel(plugin, "kottie:sync", listener)
 * // listener → bridge.onPluginMessageReceived("kottie:sync", playerId, data)
 * bridge.outboundSink = OutboundSink { channel, playerId, data ->
 *     // player.sendPluginMessage(plugin, channel, data)
 * }
 * ```
 */
class PluginChannelPacketBridge(
    private val packetBus: InMemoryPacketBus = InMemoryPacketBus(
        adapterName = "paper-plugin-channel",
        supportedVersions = setOf("plugin-messaging")
    )
) {
    private val log = LoggerFactory.getLogger(PluginChannelPacketBridge::class.java)

    /** Logical channel name → PacketType */
    private val channelTypes = ConcurrentHashMap<String, PacketType>()

    /**
     * Host-provided sender for OUTBOUND payloads after handlers run
     * (and if not cancelled).
     */
    @Volatile
    var outboundSink: OutboundSink? = null

    val bus: PacketBus get() = packetBus

    /**
     * Register a plugin channel so inbound/outbound share a stable [PacketType].
     */
    fun registerChannel(channel: String): PacketType {
        require(channel.isNotBlank()) { "channel must not be blank" }
        return channelTypes.computeIfAbsent(channel) {
            object : PacketType {
                override val id: String = "plugin-channel:$channel"
            }
        }
    }

    fun typeFor(channel: String): PacketType? = channelTypes[channel]

    /**
     * Called by the host when a plugin message arrives from a player (INBOUND).
     * @return dispatch result (cancelled / replaced payload)
     */
    fun onPluginMessageReceived(
        channel: String,
        playerId: UUID?,
        data: ByteArray
    ): InMemoryPacketBus.DispatchResult {
        val type = channelTypes[channel] ?: registerChannel(channel)
        return packetBus.dispatch(
            type = type,
            direction = PacketDirection.INBOUND,
            payload = PluginMessagePayload(channel, data.copyOf()),
            playerId = playerId
        )
    }

    /**
     * Dispatch an outbound plugin message through the bus, then optionally
     * send via [outboundSink] if not cancelled.
     */
    fun sendPluginMessage(
        channel: String,
        playerId: UUID?,
        data: ByteArray
    ): InMemoryPacketBus.DispatchResult {
        val type = channelTypes[channel] ?: registerChannel(channel)
        val result = packetBus.dispatch(
            type = type,
            direction = PacketDirection.OUTBOUND,
            payload = PluginMessagePayload(channel, data.copyOf()),
            playerId = playerId
        )
        if (!result.cancelled) {
            val payload = result.payload as? PluginMessagePayload ?: PluginMessagePayload(channel, data)
            try {
                outboundSink?.send(payload.channel, playerId, payload.data)
            } catch (t: Throwable) {
                log.error("Outbound plugin message sink failed for {}", channel, t)
            }
        }
        return result
    }

    fun interface OutboundSink {
        fun send(channel: String, playerId: UUID?, data: ByteArray)
    }
}

/**
 * Opaque payload for plugin-channel packets.
 */
data class PluginMessagePayload(
    val channel: String,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PluginMessagePayload) return false
        return channel == other.channel && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * channel.hashCode() + data.contentHashCode()
}
