package io.kottie.core.event

import io.kottie.api.event.EventBus
import io.kottie.api.event.KottieEvent
import io.kottie.api.event.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simple in-memory event bus for the initial runtime.
 * Uses a SharedFlow per event type with a modest buffer.
 * Does not silently drop decision events; overflow is reported.
 */
class DefaultEventBus(
    private val scope: CoroutineScope
) : EventBus {

    private val log = LoggerFactory.getLogger(DefaultEventBus::class.java)
    private val flows = ConcurrentHashMap<Class<out KottieEvent>, MutableSharedFlow<KottieEvent>>()

    @Suppress("UNCHECKED_CAST")
    private fun <E : KottieEvent> flowFor(type: Class<E>): MutableSharedFlow<E> {
        return flows.computeIfAbsent(type) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST // observation events; decision events need different handling later
            )
        } as MutableSharedFlow<E>
    }

    override fun <E : KottieEvent> subscribe(
        type: Class<E>,
        handler: suspend (E) -> Unit
    ): Subscription {
        val flow = flowFor(type)
        val job = scope.launch {
            flow.collect { event ->
                try {
                    handler(event)
                } catch (t: Throwable) {
                    log.error("Event handler for ${type.simpleName} failed", t)
                    // isolation: one bad handler does not kill the bus
                }
            }
        }
        return JobSubscription(job)
    }

    override suspend fun <E : KottieEvent> emit(event: E) {
        val flow = flowFor(event.javaClass)
        val emitted = flow.tryEmit(event)
        if (!emitted) {
            log.warn("Event bus buffer full for ${event.javaClass.simpleName}; event may have been dropped")
        }
    }

    private class JobSubscription(private val job: Job) : Subscription {
        private val closed = AtomicBoolean(false)
        override val isClosed: Boolean get() = closed.get() || !job.isActive
        override fun close() {
            if (closed.compareAndSet(false, true)) {
                job.cancel()
            }
        }
    }
}
