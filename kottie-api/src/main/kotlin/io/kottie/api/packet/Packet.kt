package io.kottie.api.packet

/**
 * Direction of a packet relative to the server.
 */
enum class PacketDirection {
    /** Client → server */
    INBOUND,
    /** Server → client */
    OUTBOUND
}

/**
 * Stable, version-neutral packet identity.
 * Adapters map protocol-specific packets to these types.
 * Never expose raw NMS/Paper packet classes on the public API.
 */
interface PacketType {
    val id: String
}

/**
 * A packet event delivered to handlers.
 *
 * [payload] is an opaque, adapter-owned representation. Handlers must not
 * retain it after the callback returns unless [PacketContext.claimOwnership]
 * was called.
 */
interface PacketEvent {
    val type: PacketType
    val direction: PacketDirection
    val playerId: java.util.UUID?
    /** Opaque payload; interpret only via the adapter/codec for this type. */
    val payload: Any
}

/**
 * Per-handler context for a single packet delivery.
 *
 * Constraints:
 * - Handlers must not block the network / packet event loop.
 * - Cancellation and replacement are only valid during the handler call.
 */
interface PacketContext {
    val event: PacketEvent

    /** Prevent the packet from continuing to the next stage / client / server. */
    fun cancel()

    val isCancelled: Boolean

    /**
     * Replace the outbound/inbound payload with an adapter-specific object.
     * Only valid for types the active adapter understands.
     */
    fun replace(payload: Any)

    /**
     * Claim ownership of the current payload buffer so it may be retained
     * after the handler returns. Without this, the adapter may recycle buffers.
     */
    fun claimOwnership()
}

/**
 * Handler for packet events. Must be non-blocking.
 */
fun interface PacketHandler {
    fun handle(ctx: PacketContext)
}

/**
 * Registration handle; close to unregister.
 */
interface PacketSubscription : AutoCloseable {
    val isClosed: Boolean
    override fun close()
}

/**
 * Packet interception API.
 *
 * Optional, version-sensitive module. Callers must handle
 * [UnsupportedPacketVersionException] when the adapter does not support
 * the running server version.
 */
interface PacketBus {
    /**
     * Subscribe to packets of the given type and direction.
     * @param priority higher runs earlier (default 0)
     */
    fun subscribe(
        type: PacketType,
        direction: PacketDirection,
        priority: Int = 0,
        handler: PacketHandler
    ): PacketSubscription

    /**
     * Subscribe to all packets in a direction (use sparingly).
     */
    fun subscribeAll(
        direction: PacketDirection,
        priority: Int = 0,
        handler: PacketHandler
    ): PacketSubscription

    /**
     * Adapter / version metadata.
     */
    val supportedVersions: Set<String>
    val adapterName: String
}

class UnsupportedPacketVersionException(
    message: String
) : IllegalStateException(message)
