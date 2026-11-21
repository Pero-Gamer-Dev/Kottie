package io.kottie.testkit

import io.kottie.adapter.paper.PluginChannelPacketBridge
import io.kottie.adapter.paper.PluginMessagePayload
import io.kottie.api.packet.PacketDirection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class PluginChannelPacketBridgeTest {

    @Test
    fun `inbound plugin message reaches subscriber`() {
        val bridge = PluginChannelPacketBridge()
        val type = bridge.registerChannel("kottie:sync")
        val seen = CopyOnWriteArrayList<String>()
        bridge.bus.subscribe(type, PacketDirection.INBOUND) { ctx ->
            val p = ctx.event.payload as PluginMessagePayload
            seen.add(p.data.decodeToString())
        }
        val id = UUID.randomUUID()
        val result = bridge.onPluginMessageReceived("kottie:sync", id, "hello".encodeToByteArray())
        assertFalse(result.cancelled)
        assertEquals(listOf("hello"), seen.toList())
        assertEquals(id, null) // playerId on event — check via handler if needed
    }

    @Test
    fun `cancel prevents outbound sink`() {
        val bridge = PluginChannelPacketBridge()
        val type = bridge.registerChannel("kottie:chat")
        val sent = AtomicReference<String?>(null)
        bridge.outboundSink = PluginChannelPacketBridge.OutboundSink { _, _, data ->
            sent.set(data.decodeToString())
        }
        bridge.bus.subscribe(type, PacketDirection.OUTBOUND) { ctx ->
            ctx.cancel()
        }
        bridge.sendPluginMessage("kottie:chat", null, "nope".encodeToByteArray())
        assertNull(sent.get())
    }

    @Test
    fun `outbound sink receives replaced payload`() {
        val bridge = PluginChannelPacketBridge()
        val type = bridge.registerChannel("kottie:data")
        val sent = AtomicReference<String?>(null)
        bridge.outboundSink = PluginChannelPacketBridge.OutboundSink { ch, _, data ->
            assertEquals("kottie:data", ch)
            sent.set(data.decodeToString())
        }
        bridge.bus.subscribe(type, PacketDirection.OUTBOUND) { ctx ->
            ctx.replace(PluginMessagePayload("kottie:data", "filtered".encodeToByteArray()))
        }
        bridge.sendPluginMessage("kottie:data", UUID.randomUUID(), "raw".encodeToByteArray())
        assertEquals("filtered", sent.get())
    }
}
