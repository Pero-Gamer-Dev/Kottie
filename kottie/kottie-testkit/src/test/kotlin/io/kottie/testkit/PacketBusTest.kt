package io.kottie.testkit

import io.kottie.api.packet.PacketDirection
import io.kottie.packets.CommonPacketTypes
import io.kottie.packets.InMemoryPacketBus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class PacketBusTest {

    @Test
    fun `inbound handler receives chat packet`() {
        val bus = InMemoryPacketBus()
        val seen = CopyOnWriteArrayList<String>()
        bus.subscribe(CommonPacketTypes.CHAT, PacketDirection.INBOUND) { ctx ->
            seen.add(ctx.event.payload as String)
        }
        val result = bus.dispatch(
            CommonPacketTypes.CHAT,
            PacketDirection.INBOUND,
            "hello",
            playerId = UUID.randomUUID()
        )
        assertFalse(result.cancelled)
        assertEquals(listOf("hello"), seen.toList())
    }

    @Test
    fun `cancel stops later handlers`() {
        val bus = InMemoryPacketBus()
        val order = CopyOnWriteArrayList<String>()
        bus.subscribe(CommonPacketTypes.CHAT, PacketDirection.INBOUND, priority = 10) { ctx ->
            order.add("high")
            ctx.cancel()
        }
        bus.subscribe(CommonPacketTypes.CHAT, PacketDirection.INBOUND, priority = 0) { ctx ->
            order.add("low")
        }
        val result = bus.dispatch(CommonPacketTypes.CHAT, PacketDirection.INBOUND, "x")
        assertTrue(result.cancelled)
        assertEquals(listOf("high"), order.toList())
    }

    @Test
    fun `replace changes payload`() {
        val bus = InMemoryPacketBus()
        bus.subscribe(CommonPacketTypes.CHAT, PacketDirection.OUTBOUND) { ctx ->
            ctx.replace("filtered")
        }
        val result = bus.dispatch(CommonPacketTypes.CHAT, PacketDirection.OUTBOUND, "raw")
        assertEquals("filtered", result.payload)
    }

    @Test
    fun `subscription close stops delivery`() {
        val bus = InMemoryPacketBus()
        val count = AtomicInteger(0)
        val sub = bus.subscribe(CommonPacketTypes.POSITION, PacketDirection.INBOUND) {
            count.incrementAndGet()
        }
        bus.dispatch(CommonPacketTypes.POSITION, PacketDirection.INBOUND, "a")
        assertEquals(1, count.get())
        sub.close()
        assertTrue(sub.isClosed)
        bus.dispatch(CommonPacketTypes.POSITION, PacketDirection.INBOUND, "b")
        assertEquals(1, count.get())
    }

    @Test
    fun `direction filter works`() {
        val bus = InMemoryPacketBus()
        val inbound = AtomicInteger(0)
        val outbound = AtomicInteger(0)
        bus.subscribe(CommonPacketTypes.CHAT, PacketDirection.INBOUND) { inbound.incrementAndGet() }
        bus.subscribe(CommonPacketTypes.CHAT, PacketDirection.OUTBOUND) { outbound.incrementAndGet() }

        bus.dispatch(CommonPacketTypes.CHAT, PacketDirection.INBOUND, "i")
        bus.dispatch(CommonPacketTypes.CHAT, PacketDirection.OUTBOUND, "o")

        assertEquals(1, inbound.get())
        assertEquals(1, outbound.get())
    }

    @Test
    fun `handler exceptions are isolated`() {
        val bus = InMemoryPacketBus()
        val reached = AtomicInteger(0)
        bus.subscribe(CommonPacketTypes.CHAT, PacketDirection.INBOUND, priority = 10) {
            error("boom")
        }
        bus.subscribe(CommonPacketTypes.CHAT, PacketDirection.INBOUND, priority = 0) {
            reached.incrementAndGet()
        }
        bus.dispatch(CommonPacketTypes.CHAT, PacketDirection.INBOUND, "x")
        assertEquals(1, reached.get())
    }

    @Test
    fun `adapter metadata present`() {
        val bus = InMemoryPacketBus()
        assertEquals("in-memory", bus.adapterName)
        assertTrue(bus.supportedVersions.contains("test"))
    }
}
