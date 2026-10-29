package io.kottie.testkit

import io.kottie.api.Kottie
import io.kottie.api.ecs.components.ManaComponent
import io.kottie.api.event.player.PlayerJoinEvent
import io.kottie.api.event.subscribe
import io.kottie.core.KottieRuntime
import io.kottie.ecs.SimpleEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class RuntimeSmokeTest {

    @Test
    fun `runtime starts and exposes services`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            assertEquals(io.kottie.api.lifecycle.LifecycleState.RUNNING, runtime.state)
            assertNotNull(Kottie.get())
            assertFalse(runtime.scheduler.isServerThread()) // test thread is not server thread
        } finally {
            runtime.onDisable()
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

            // give the collector a moment
            delay(100)

            val got = received.get()
            assertNotNull(got)
            assertEquals("TestPlayer", got!!.name)
            assertTrue(got.entity.has(ManaComponent.KEY))
            assertEquals(100, got.entity.get(ManaComponent.KEY)!!.currentMana)

            sub.close()
        } finally {
            runtime.onDisable()
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
        } finally {
            runtime.onDisable()
        }
    }
}
