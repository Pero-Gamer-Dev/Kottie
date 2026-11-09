package io.kottie.api.event.test

import io.kottie.api.event.EventCategory
import io.kottie.api.event.KottieEvent

/**
 * Sample decision event for tests and examples.
 * Handlers run sequentially; emit() waits for completion.
 */
data class CancellableActionEvent(
    val actionId: String,
    var cancelled: Boolean = false
) : KottieEvent {
    override val category: EventCategory get() = EventCategory.DECISION

    fun cancel() {
        cancelled = true
    }
}

/**
 * Sample observation event that can be used to stress the buffer.
 */
data class TickObservationEvent(
    val tick: Long
) : KottieEvent {
    override val category: EventCategory get() = EventCategory.OBSERVATION
}
