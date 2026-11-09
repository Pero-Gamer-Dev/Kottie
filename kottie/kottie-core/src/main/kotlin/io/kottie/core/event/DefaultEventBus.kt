package io.kottie.core.event

import io.kottie.api.event.EventBus
import io.kottie.api.event.EventBusMetrics
import io.kottie.api.event.EventCategory
import io.kottie.api.event.KottieEvent
import io.kottie.api.event.Subscription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Event bus implementing observation vs decision delivery contracts.
 *
 * Observation: SharedFlow with bounded buffer (DROP_OLDEST under overload).
 * Decision: per-type handler list, sequential invocation under a mutex;
 *           emit() suspends until all handlers complete.
 */
class DefaultEventBus(
    private val scope: CoroutineScope
) : EventBus {

    private val log = LoggerFactory.getLogger(DefaultEventBus::class.java)

    private val emitted = AtomicLong(0)
    private val delivered = AtomicLong(0)
    private val dropped = AtomicLong(0)
    private val handlerFailures = AtomicLong(0)
    private val activeSubs = AtomicInteger(0)

    // Observation path
    private val observationFlows =
        ConcurrentHashMap<Class<out KottieEvent>, MutableSharedFlow<KottieEvent>>()

    // Decision path: ordered handler lists
    private data class DecisionHandler(
        val priority: Int,
        val order: Long,
        val handler: suspend (KottieEvent) -> Unit,
        val job: Job? = null // unused for decision; kept for symmetry
    )

    private val decisionHandlers =
        ConcurrentHashMap<Class<out KottieEvent>, CopyOnWriteArrayList<DecisionHandler>>()
    private val decisionMutexes =
        ConcurrentHashMap<Class<out KottieEvent>, Mutex>()
    private val registrationOrder = AtomicLong(0)

    @Suppress("UNCHECKED_CAST")
    private fun observationFlow(type: Class<out KottieEvent>): MutableSharedFlow<KottieEvent> {
        return observationFlows.computeIfAbsent(type) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = 64,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
        }
    }

    override fun <E : KottieEvent> subscribe(
        type: Class<E>,
        priority: Int,
        handler: suspend (E) -> Unit
    ): Subscription {
        // We don't know the category of future events at subscribe time, so we
        // register for both paths. Observation uses a collector job; Decision
        // uses an ordered list.
        activeSubs.incrementAndGet()

        val observationJob = scope.launch {
            observationFlow(type).collect { event ->
                if (event.category != EventCategory.OBSERVATION) return@collect
                @Suppress("UNCHECKED_CAST")
                try {
                    handler(event as E)
                    delivered.incrementAndGet()
                } catch (t: Throwable) {
                    handlerFailures.incrementAndGet()
                    log.error("Observation handler for ${type.simpleName} failed", t)
                }
            }
        }

        val decisionList = decisionHandlers.computeIfAbsent(type) { CopyOnWriteArrayList() }
        @Suppress("UNCHECKED_CAST")
        val decisionHandler = DecisionHandler(
            priority = priority,
            order = registrationOrder.getAndIncrement(),
            handler = handler as suspend (KottieEvent) -> Unit
        )
        decisionList.add(decisionHandler)
        // Keep sorted by priority desc, then registration order asc
        decisionList.sortWith(compareByDescending<DecisionHandler> { it.priority }.thenBy { it.order })

        return object : Subscription {
            private val closed = AtomicBoolean(false)
            override val isClosed: Boolean get() = closed.get()
            override fun close() {
                if (closed.compareAndSet(false, true)) {
                    observationJob.cancel()
                    decisionList.remove(decisionHandler)
                    activeSubs.decrementAndGet()
                }
            }
        }
    }

    override suspend fun <E : KottieEvent> emit(event: E) {
        emitted.incrementAndGet()
        when (event.category) {
            EventCategory.OBSERVATION -> emitObservation(event)
            EventCategory.DECISION -> emitDecision(event)
        }
    }

    private suspend fun emitObservation(event: KottieEvent) {
        val flow = observationFlow(event.javaClass)
        val ok = flow.tryEmit(event)
        if (!ok) {
            dropped.incrementAndGet()
            log.warn("Observation event dropped (buffer full): {}", event.javaClass.simpleName)
        }
        // delivered is counted inside the collector after successful handler run
    }

    private suspend fun emitDecision(event: KottieEvent) {
        val type = event.javaClass
        val handlers = decisionHandlers[type] ?: return
        val mutex = decisionMutexes.computeIfAbsent(type) { Mutex() }
        mutex.withLock {
            // snapshot to avoid concurrent modification while iterating
            val snapshot = handlers.toList()
            for (h in snapshot) {
                try {
                    h.handler(event)
                    delivered.incrementAndGet()
                } catch (t: Throwable) {
                    handlerFailures.incrementAndGet()
                    log.error("Decision handler for ${type.simpleName} failed", t)
                    // isolation: continue with remaining handlers
                }
            }
        }
    }

    override fun metrics(): EventBusMetrics = EventBusMetrics(
        emitted = emitted.get(),
        delivered = delivered.get(),
        dropped = dropped.get(),
        handlerFailures = handlerFailures.get(),
        activeSubscriptions = activeSubs.get()
    )
}
