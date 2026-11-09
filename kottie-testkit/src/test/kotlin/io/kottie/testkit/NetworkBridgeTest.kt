package io.kottie.testkit

import io.kottie.api.network.NetworkMessage
import io.kottie.api.network.NetworkState
import io.kottie.api.network.OutagePolicy
import io.kottie.api.network.ReconnectPolicy
import io.kottie.network.InMemoryNetworkBridge
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class NetworkBridgeTest {

    @Test
    fun `publish delivers to subscribers when connected`() = runBlocking {
        val bridge = InMemoryNetworkBridge()
        bridge.connect()
        val received = CopyOnWriteArrayList<String>()
        bridge.subscribe("chat") { msg ->
            received.add(msg.payload.decodeToString())
        }
        bridge.publish(
            NetworkMessage(channel = "chat", payload = "hi".encodeToByteArray())
        )
        assertEquals(listOf("hi"), received.toList())
        bridge.disconnect()
    }

    @Test
    fun `publish fails when disconnected`() = runBlocking {
        val bridge = InMemoryNetworkBridge()
        assertEquals(NetworkState.DISCONNECTED, bridge.state)
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                bridge.publish(NetworkMessage("c", "x".encodeToByteArray()))
            }
        }
    }

    @Test
    fun `duplicate idempotency key is dropped`() = runBlocking {
        val bridge = InMemoryNetworkBridge()
        bridge.connect()
        val count = AtomicInteger(0)
        bridge.subscribe("inv") { count.incrementAndGet() }

        val msg = NetworkMessage(
            channel = "inv",
            payload = "data".encodeToByteArray(),
            idempotencyKey = "same-key"
        )
        bridge.publish(msg)
        bridge.receive(msg.copy()) // same key
        assertEquals(1, count.get())
    }

    @Test
    fun `stale messages dropped under DROP_STALE`() = runBlocking {
        val bridge = InMemoryNetworkBridge(outagePolicy = OutagePolicy.DROP_STALE)
        bridge.connect()
        val payloads = CopyOnWriteArrayList<String>()
        bridge.subscribe("state") { payloads.add(it.payload.decodeToString()) }

        val newer = NetworkMessage(
            channel = "state",
            payload = "new".encodeToByteArray(),
            sentAtEpochMs = 2000L,
            idempotencyKey = "k1"
        )
        val older = NetworkMessage(
            channel = "state",
            payload = "old".encodeToByteArray(),
            sentAtEpochMs = 1000L,
            idempotencyKey = "k2"
        )
        bridge.publish(newer)
        bridge.receive(older)
        assertEquals(listOf("new"), payloads.toList())
    }

    @Test
    fun `reconnect policy delay grows`() {
        val policy = ReconnectPolicy(
            initialDelay = Duration.ofMillis(100),
            maxDelay = Duration.ofSeconds(1),
            multiplier = 2.0
        )
        assertEquals(100, policy.delayForAttempt(0).toMillis())
        assertEquals(200, policy.delayForAttempt(1).toMillis())
        assertEquals(400, policy.delayForAttempt(2).toMillis())
        assertEquals(1000, policy.delayForAttempt(10).toMillis()) // capped
    }

    @Test
    fun `simulate disconnect then connect restores publish`() = runBlocking {
        val bridge = InMemoryNetworkBridge()
        bridge.connect()
        bridge.simulateDisconnect()
        assertEquals(NetworkState.DISCONNECTED, bridge.state)
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                bridge.publish(NetworkMessage("c", "x".encodeToByteArray()))
            }
        }
        bridge.connect()
        val ok = AtomicInteger(0)
        bridge.subscribe("c") { ok.incrementAndGet() }
        bridge.publish(NetworkMessage("c", "y".encodeToByteArray(), idempotencyKey = "y1"))
        assertEquals(1, ok.get())
    }
}
