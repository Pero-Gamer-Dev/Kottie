package io.kottie.packets

import io.kottie.api.packet.PacketBus
import io.kottie.api.packet.PacketContext
import io.kottie.api.packet.PacketDirection
import io.kottie.api.packet.PacketEvent
import io.kottie.api.packet.PacketHandler
import io.kottie.api.packet.PacketSubscription
import io.kottie.api.packet.PacketType
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Test / development packet bus. Not bound to a real Minecraft protocol.
 * Production adapters (e.g. Paper) implement [PacketBus] against the
 * server's network pipeline and must never block the netty event loop.
 */
class InMemoryPacketBus(
    override val adapterName: String = "in-memory",
    override val supportedVersions: Set<String> = setOf("test")
) : PacketBus {

    private val log = LoggerFactory.getLogger(InMemoryPacketBus::class.java)
    private val registrationOrder = AtomicLong(0)

    private data class Reg(
        val type: PacketType?,
        val direction: PacketDirection,
        val priority: Int,
        val order: Long,
        val handler: PacketHandler
    )

    private val handlers = CopyOnWriteArrayList<Reg>()

    override fun subscribe(
        type: PacketType,
        direction: PacketDirection,
        priority: Int,
        handler: PacketHandler
    ): PacketSubscription = add(type, direction, priority, handler)

    override fun subscribeAll(
        direction: PacketDirection,
        priority: Int,
        handler: PacketHandler
    ): PacketSubscription = add(null, direction, priority, handler)

    private fun add(
        type: PacketType?,
        direction: PacketDirection,
        priority: Int,
        handler: PacketHandler
    ): PacketSubscription {
        val reg = Reg(type, direction, priority, registrationOrder.getAndIncrement(), handler)
        handlers.add(reg)
        sortHandlers()
        return object : PacketSubscription {
            private val closed = AtomicBoolean(false)
            override val isClosed: Boolean get() = closed.get()
            override fun close() {
                if (closed.compareAndSet(false, true)) {
                    handlers.remove(reg)
                }
            }
        }
    }

    private fun sortHandlers() {
        handlers.sortWith(
            compareByDescending<Reg> { it.priority }.thenBy { it.order }
        )
    }

    /**
     * Inject a packet for tests. Runs matching handlers synchronously
     * (still non-blocking by contract — handlers must not block).
     */
    fun dispatch(
        type: PacketType,
        direction: PacketDirection,
        payload: Any,
        playerId: UUID? = null
    ): DispatchResult {
        val event = object : PacketEvent {
            override val type: PacketType = type
            override val direction: PacketDirection = direction
            override val playerId: UUID? = playerId
            override val payload: Any = payload
        }
        var cancelled = false
        var currentPayload: Any = payload
        var ownershipClaimed = false

        val ctx = object : PacketContext {
            override val event: PacketEvent get() = event
            override fun cancel() {
                cancelled = true
            }
            override val isCancelled: Boolean get() = cancelled
            override fun replace(payload: Any) {
                currentPayload = payload
            }
            override fun claimOwnership() {
                ownershipClaimed = true
            }
        }

        val snapshot = handlers.toList()
        for (reg in snapshot) {
            if (reg.direction != direction) continue
            if (reg.type != null && reg.type != type) continue
            try {
                reg.handler.handle(ctx)
            } catch (t: Throwable) {
                log.error("Packet handler failed for {}", type.id, t)
            }
            if (cancelled) break
        }

        return DispatchResult(
            cancelled = cancelled,
            payload = currentPayload,
            ownershipClaimed = ownershipClaimed
        )
    }

    data class DispatchResult(
        val cancelled: Boolean,
        val payload: Any,
        val ownershipClaimed: Boolean
    )
}

/** Example stable packet types for tests and demos. */
object CommonPacketTypes {
    val CHAT = object : PacketType {
        override val id: String = "kottie:chat"
    }
    val POSITION = object : PacketType {
        override val id: String = "kottie:position"
    }
}
