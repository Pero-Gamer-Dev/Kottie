package io.kottie.testkit

import io.kottie.adapter.paper.PaperEventBridge
import io.kottie.adapter.paper.PaperServerAdapter
import io.kottie.adapter.paper.PaperServerScheduler
import io.kottie.adapter.paper.UnsupportedServerEnvironmentException
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
import java.util.concurrent.atomic.AtomicReference

class PaperAdapterTest {

    @Test
    fun `adapter fails without scheduler`() {
        val adapter = PaperServerAdapter()
        assertThrows(UnsupportedServerEnvironmentException::class.java) {
            adapter.initialize()
        }
    }

    @Test
    fun `adapter initializes with paper scheduler`() {
        val exec = Executors.newSingleThreadExecutor()
        val scheduler = PaperServerScheduler(
            mainThreadExecutor = exec,
            isPrimaryThread = { false }
        )
        val adapter = PaperServerAdapter(schedulerImpl = scheduler, liveServerDetected = true)
        adapter.initialize()
        assertEquals("paper", adapter.serverName)
        assertTrue(adapter.supportedVersions.contains("1.21"))
        adapter.shutdown()
        exec.shutdownNow()
    }

    @Test
    fun `forPaperHost factory works`() {
        val exec = Executors.newSingleThreadExecutor()
        val adapter = PaperServerAdapter.forPaperHost(
            mainThreadExecutor = exec,
            isPrimaryThread = { Thread.currentThread().name.startsWith("pool") }
        )
        adapter.initialize()
        assertNotNull(adapter.scheduler)
        adapter.shutdown()
        exec.shutdownNow()
    }

    @Test
    fun `event bridge emits join and quit`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val adapter = PaperServerAdapter(schedulerImpl = InMemoryServerScheduler())
            adapter.initialize()
            val bridge = adapter.attachEventBridge(
                runtime.eventBus,
                runtime.entityStore,
                runtime.scope
            )

            val joined = AtomicReference<PlayerJoinEvent?>(null)
            val quit = AtomicReference<PlayerQuitEvent?>(null)
            runtime.eventBus.subscribe<PlayerJoinEvent> { joined.set(it) }
            runtime.eventBus.subscribe<PlayerQuitEvent> { quit.set(it) }

            val id = UUID.randomUUID()
            bridge.onPlayerJoin(id, "Steve")
            delay(100)
            assertNotNull(joined.get())
            assertEquals("Steve", joined.get()!!.name)
            assertEquals(id, joined.get()!!.playerId)
            assertNotNull(bridge.entityFor(id))

            bridge.onPlayerQuit(id, "Steve")
            delay(100)
            assertNotNull(quit.get())
            assertEquals("Steve", quit.get()!!.name)
            assertNull(bridge.entityFor(id))

            adapter.shutdown()
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `paper scheduler runs block on executor thread`() = runBlocking {
        var ranOn: String? = null
        val exec = Executors.newSingleThreadExecutor { r ->
            Thread(r, "fake-main").apply { isDaemon = true }
        }
        val scheduler = PaperServerScheduler(
            mainThreadExecutor = exec,
            isPrimaryThread = { Thread.currentThread().name == "fake-main" }
        )
        // allow init capture
        delay(50)
        scheduler.onServerThread {
            ranOn = Thread.currentThread().name
        }
        assertEquals("fake-main", ranOn)
        scheduler.shutdown()
        exec.shutdownNow()
    }
}
