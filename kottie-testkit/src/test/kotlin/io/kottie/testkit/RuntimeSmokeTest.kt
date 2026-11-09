package io.kottie.testkit

import io.kottie.api.Kottie
import io.kottie.api.diagnostics.DiagnosticEvent
import io.kottie.api.diagnostics.DiagnosticSink
import io.kottie.api.ecs.components.ManaComponent
import io.kottie.api.event.player.PlayerJoinEvent
import io.kottie.api.event.subscribe
import io.kottie.api.lifecycle.LifecycleState
import io.kottie.core.KottieRuntime
import io.kottie.ecs.SimpleEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class RuntimeSmokeTest {

    @Test
    fun `runtime starts and exposes services`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            assertEquals(LifecycleState.RUNNING, runtime.state)
            assertNotNull(Kottie.get())
            assertFalse(runtime.scheduler.isServerThread())
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
            assertEquals(LifecycleState.STOPPED, runtime.state)
        }
    }

    @Test
    fun `event bus delivers player join`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val received = AtomicReference<PlayerJoinEvent?>(null)
            val sub = runtime.eventBus.subscribe<PlayerJoinEvent> { event ->
                received.set(event)
            }

            val entity = SimpleEntity()
            entity.set(ManaComponent.KEY, ManaComponent(100, 100))

            val event = PlayerJoinEvent(
                playerId = UUID.randomUUID(),
                name = "TestPlayer",
                entity = entity
            )
            runtime.eventBus.emit(event)
            delay(150)

            val got = received.get()
            assertNotNull(got)
            assertEquals("TestPlayer", got!!.name)
            assertTrue(got.entity.has(ManaComponent.KEY))
            assertEquals(100, got.entity.get(ManaComponent.KEY)!!.currentMana)
            sub.close()
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `server thread affinity works`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            var onServer = false
            runtime.scheduler.onServerThread {
                onServer = runtime.scheduler.isServerThread()
            }
            assertTrue(onServer)

            // requireServerThread should not throw on server thread
            runtime.scheduler.onServerThread {
                runtime.scheduler.requireServerThread("test")
            }

            // requireServerThread should throw off server thread
            assertThrows(IllegalStateException::class.java) {
                runtime.scheduler.requireServerThread("must fail")
            }
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `jobs are cancelled on shutdown`() = runBlocking {
        val runtime = KottieRuntime.start()
        val stillActiveAfterDisable = AtomicReference(true)

        val job = runtime.scope.launch {
            try {
                while (isActive) {
                    delay(50)
                }
            } finally {
                stillActiveAfterDisable.set(isActive)
            }
        }

        delay(100)
        assertTrue(job.isActive)

        runtime.onDisable(Duration.ofSeconds(2))
        delay(50)

        assertFalse(job.isActive)
        assertFalse(stillActiveAfterDisable.get())
        assertEquals(LifecycleState.STOPPED, runtime.state)
    }

    @Test
    fun `diagnostics are emitted for lifecycle and cancellation`() = runBlocking {
        val captured = CopyOnWriteArrayList<DiagnosticEvent>()
        val sink = DiagnosticSink { captured.add(it) }

        val runtime = KottieRuntime.start(diagnosticSink = sink)
        try {
            assertTrue(captured.any { it.category == DiagnosticEvent.Category.LIFECYCLE })
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }

        assertTrue(captured.any { it.category == DiagnosticEvent.Category.SHUTDOWN })
        assertTrue(captured.any { it.category == DiagnosticEvent.Category.CANCELLATION })
        assertTrue(captured.any { it.category == DiagnosticEvent.Category.LIFECYCLE && it.message.contains("stopped") })
    }
}
