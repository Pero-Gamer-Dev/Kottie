package io.kottie.testkit

import io.kottie.api.event.EventCategory
import io.kottie.api.event.subscribe
import io.kottie.api.event.test.CancellableActionEvent
import io.kottie.api.event.test.TickObservationEvent
import io.kottie.core.KottieRuntime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class EventEngineTest {

    @Test
    fun `decision handlers run in priority order and emit waits`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val order = CopyOnWriteArrayList<String>()

            runtime.eventBus.subscribe<CancellableActionEvent>(priority = 10) { e ->
                order.add("high")
                delay(20)
            }
            runtime.eventBus.subscribe<CancellableActionEvent>(priority = 0) { e ->
                order.add("normal")
            }
            runtime.eventBus.subscribe<CancellableActionEvent>(priority = 5) { e ->
                order.add("mid")
            }

            val event = CancellableActionEvent("act-1")
            runtime.eventBus.emit(event)

            // emit should have waited for all handlers
            assertEquals(listOf("high", "mid", "normal"), order.toList())
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `handler exceptions are isolated`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val reached = AtomicInteger(0)

            runtime.eventBus.subscribe<CancellableActionEvent>(priority = 10) {
                throw RuntimeException("boom")
            }
            runtime.eventBus.subscribe<CancellableActionEvent>(priority = 0) {
                reached.incrementAndGet()
            }

            runtime.eventBus.emit(CancellableActionEvent("act-2"))

            assertEquals(1, reached.get())
            assertTrue(runtime.eventBus.metrics().handlerFailures >= 1)
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `subscription close stops delivery`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val count = AtomicInteger(0)
            val sub = runtime.eventBus.subscribe<TickObservationEvent> {
                count.incrementAndGet()
            }

            runtime.eventBus.emit(TickObservationEvent(1))
            delay(80)
            assertEquals(1, count.get())

            sub.close()
            assertTrue(sub.isClosed)

            runtime.eventBus.emit(TickObservationEvent(2))
            delay(80)
            assertEquals(1, count.get()) // no further delivery
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `observation events are classified correctly`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val event = TickObservationEvent(42)
            assertEquals(EventCategory.OBSERVATION, event.category)

            val decision = CancellableActionEvent("x")
            assertEquals(EventCategory.DECISION, decision.category)
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `metrics track emissions`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val before = runtime.eventBus.metrics()
            runtime.eventBus.emit(TickObservationEvent(1))
            runtime.eventBus.emit(CancellableActionEvent("m"))
            delay(50)
            val after = runtime.eventBus.metrics()
            assertTrue(after.emitted >= before.emitted + 2)
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }
}
