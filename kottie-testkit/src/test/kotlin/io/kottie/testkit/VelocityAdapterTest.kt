package io.kottie.testkit

import io.kottie.adapter.velocity.UnsupportedProxyEnvironmentException
import io.kottie.adapter.velocity.VelocityServerAdapter
import io.kottie.adapter.velocity.VelocityServerScheduler
import io.kottie.api.event.player.PlayerJoinEvent
import io.kottie.api.event.player.PlayerQuitEvent
import io.kottie.api.event.subscribe
import io.kottie.core.KottieRuntime
import io.kottie.core.scheduler.InMemoryServerScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class VelocityAdapterTest {

    @Test
    fun `fails without scheduler`() {
        val adapter = VelocityServerAdapter()
        assertThrows(UnsupportedProxyEnvironmentException::class.java) {
            adapter.initialize()
        }
    }

    @Test
    fun `forVelocityHost initializes`() {
        val exec = Executors.newSingleThreadExecutor()
        val adapter = VelocityServerAdapter.forVelocityHost(
            proxyExecutor = exec,
            isProxyThread = { true }
        )
        adapter.initialize()
        assertEquals("velocity", adapter.serverName)
        assertTrue(adapter.supportedVersions.isNotEmpty())
        adapter.shutdown()
        exec.shutdownNow()
    }

    @Test
    fun `login and disconnect emit events and track entities`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val adapter = VelocityServerAdapter(schedulerImpl = InMemoryServerScheduler())
            adapter.initialize()
            val notifyCount = AtomicInteger(0)
            val bridge = adapter.attachEventBridge(
                runtime.eventBus,
                runtime.entityStore,
                runtime.scope,
                onLoginNotify = { _, _ -> notifyCount.incrementAndGet() }
            )

            val joined = AtomicReference<PlayerJoinEvent?>(null)
            val quit = AtomicReference<PlayerQuitEvent?>(null)
            runtime.eventBus.subscribe<PlayerJoinEvent> { joined.set(it) }
            runtime.eventBus.subscribe<PlayerQuitEvent> { quit.set(it) }

            val id = UUID.randomUUID()
            bridge.onPlayerLogin(id, "ProxyPlayer")
            delay(100)
            assertNotNull(joined.get())
            assertEquals("ProxyPlayer", joined.get()!!.name)
            assertNotNull(bridge.entityFor(id))
            assertTrue(bridge.trackedPlayers().contains(id))
            assertEquals(1, notifyCount.get())

            bridge.onPlayerDisconnect(id, "ProxyPlayer")
            delay(100)
            assertNotNull(quit.get())
            assertNull(bridge.entityFor(id))

            adapter.shutdown()
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `scheduler runs on proxy executor`() = runBlocking {
        var name: String? = null
        val exec = Executors.newSingleThreadExecutor { r ->
            Thread(r, "velocity-main").apply { isDaemon = true }
        }
        val scheduler = VelocityServerScheduler(
            proxyExecutor = exec,
            isProxyThread = { Thread.currentThread().name == "velocity-main" }
        )
        delay(30)
        scheduler.onServerThread { name = Thread.currentThread().name }
        assertEquals("velocity-main", name)
        scheduler.shutdown()
        exec.shutdownNow()
    }
}
