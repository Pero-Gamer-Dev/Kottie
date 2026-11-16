package io.kottie.testkit

import io.kottie.api.cluster.InstanceId
import io.kottie.api.cluster.ServerPlatform
import io.kottie.cluster.DefaultClusterMessenger
import io.kottie.cluster.DefaultClusterPresenceService
import io.kottie.network.InMemoryNetworkBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class ClusterTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `two instances see each other via presence`() = runBlocking {
        val bridge = InMemoryNetworkBridge()
        bridge.connect()

        val aId = InstanceId("instance-a")
        val bId = InstanceId("instance-b")
        val a = DefaultClusterPresenceService(
            bridge, scope, aId, ServerPlatform.PAPER,
            heartbeatInterval = Duration.ofMillis(50),
            staleTimeout = Duration.ofSeconds(2)
        )
        val b = DefaultClusterPresenceService(
            bridge, scope, bId, ServerPlatform.FOLIA,
            heartbeatInterval = Duration.ofMillis(50),
            staleTimeout = Duration.ofSeconds(2)
        )
        a.start()
        b.start()
        delay(200)

        val aMembers = a.view.members().map { it.instanceId.value }.toSet()
        val bMembers = b.view.members().map { it.instanceId.value }.toSet()
        assertTrue(aMembers.contains("instance-a"))
        assertTrue(aMembers.contains("instance-b"), "a should see b: $aMembers")
        assertTrue(bMembers.contains("instance-a"), "b should see a: $bMembers")

        a.stop()
        b.stop()
        bridge.disconnect()
    }

    @Test
    fun `cluster messenger delivers topic payload`() = runBlocking {
        val bridge = InMemoryNetworkBridge()
        bridge.connect()
        val id = InstanceId("msg-1")
        val messenger = DefaultClusterMessenger(bridge, scope, id)
        val received = CopyOnWriteArrayList<String>()
        messenger.subscribe("sync") { env ->
            received.add(env.payload.decodeToString())
        }
        delay(30)
        messenger.publish("sync", "hello-cluster".encodeToByteArray())
        delay(80)
        assertTrue(received.contains("hello-cluster"))
        bridge.disconnect()
    }

    @Test
    fun `presence encode decode round trip`() {
        val p = io.kottie.api.cluster.InstancePresence(
            instanceId = InstanceId("x"),
            platform = ServerPlatform.PURPUR,
            startedAt = java.time.Instant.ofEpochMilli(1000),
            lastHeartbeat = java.time.Instant.ofEpochMilli(2000),
            metadata = mapOf("region" to "eu")
        )
        val bytes = DefaultClusterPresenceService.encode(p)
        val decoded = DefaultClusterPresenceService.decode(bytes)!!
        assertEquals("x", decoded.instanceId.value)
        assertEquals(ServerPlatform.PURPUR, decoded.platform)
        assertEquals("eu", decoded.metadata["region"])
    }

    @Test
    fun `messenger encode decode round trip`() {
        val env = io.kottie.api.cluster.ClusterEnvelope(
            from = InstanceId("f"),
            topic = "t",
            payload = "p".encodeToByteArray(),
            correlationId = "c1",
            sentAtEpochMs = 99L
        )
        val decoded = DefaultClusterMessenger.decode(DefaultClusterMessenger.encode(env))!!
        assertEquals("f", decoded.from.value)
        assertEquals("t", decoded.topic)
        assertEquals("c1", decoded.correlationId)
        assertEquals(99L, decoded.sentAtEpochMs)
        assertEquals("p", decoded.payload.decodeToString())
    }
}
