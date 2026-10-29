package io.kottie.api.event

/**
 * Typed event bus with explicit subscription ownership.
 *
 * Delivery guarantees, ordering, and thread context are defined per event type
 * in the runtime documentation. Decision events that affect server progress
 * run under a strict contract; observation events may be processed asynchronously
 * when the payload is a snapshot.
 */
interface EventBus {
    /**
     * Subscribe to events of the given type. The handler is invoked according
     * to the event category contract. The returned [Subscription] must be
     * closed (or the owning scope cancelled) to avoid leaks.
     */
    fun <E : KottieEvent> subscribe(
        type: Class<E>,
        handler: suspend (E) -> Unit
    ): Subscription

    /**
     * Emit an event. Suspends until delivery contracts for the event type
     * are satisfied (or fails with an exception that is reported to diagnostics).
     */
    suspend fun <E : KottieEvent> emit(event: E)
}

/**
 * Convenience reified subscribe for Kotlin callers.
 */
inline fun <reified E : KottieEvent> EventBus.subscribe(
    noinline handler: suspend (E) -> Unit
): Subscription = subscribe(E::class.java, handler)
