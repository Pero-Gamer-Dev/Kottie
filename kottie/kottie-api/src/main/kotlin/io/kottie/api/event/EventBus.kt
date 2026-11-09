package io.kottie.api.event

/**
 * Typed event bus with explicit subscription ownership and delivery contracts.
 *
 * ## Delivery guarantees (per [EventCategory])
 *
 * - **OBSERVATION**: asynchronous, bounded buffer, isolated handlers.
 *   Under overload the oldest events may be dropped; drops are counted.
 * - **DECISION**: sequential delivery for a single emission; never silently dropped.
 *   [emit] suspends until all registered handlers for that event have completed
 *   (or failed in isolation).
 *
 * Ordering: handlers for a single emission of a DECISION event run in
 * registration order. OBSERVATION events do not guarantee global order
 * across concurrent emitters.
 */
interface EventBus {

    /**
     * Subscribe to events of the given type.
     *
     * @param type event class
     * @param priority higher values run earlier for DECISION events (default 0)
     * @param handler suspending handler; exceptions are isolated and do not
     *                cancel other handlers or the bus
     */
    fun <E : KottieEvent> subscribe(
        type: Class<E>,
        priority: Int = 0,
        handler: suspend (E) -> Unit
    ): Subscription

    /**
     * Emit an event according to its [KottieEvent.category] contract.
     */
    suspend fun <E : KottieEvent> emit(event: E)

    /**
     * Snapshot of bus metrics (optional; may be zeroed if not instrumented).
     */
    fun metrics(): EventBusMetrics = EventBusMetrics()
}

/**
 * Basic counters exposed for diagnostics and tests.
 */
data class EventBusMetrics(
    val emitted: Long = 0,
    val delivered: Long = 0,
    val dropped: Long = 0,
    val handlerFailures: Long = 0,
    val activeSubscriptions: Int = 0
)

/**
 * Convenience reified subscribe for Kotlin callers.
 */
inline fun <reified E : KottieEvent> EventBus.subscribe(
    priority: Int = 0,
    noinline handler: suspend (E) -> Unit
): Subscription = subscribe(E::class.java, priority, handler)
