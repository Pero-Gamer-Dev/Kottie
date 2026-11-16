package io.kottie.api.event

/**
 * Marker interface for all Kottie events.
 *
 * Subtypes should declare a [EventCategory] via [KottieEvent.category]
 * (or rely on the default Observation classification).
 */
interface KottieEvent {
    /**
     * Delivery category for this event instance.
     * Defaults to [EventCategory.OBSERVATION].
     */
    val category: EventCategory
        get() = EventCategory.OBSERVATION
}

/**
 * Classifies how an event must be delivered.
 *
 * - [OBSERVATION]: immutable fact / snapshot. May be processed asynchronously.
 *   Bounded buffering is allowed; oldest events may be dropped under overload
 *   (with metrics). Handler failures are isolated.
 * - [DECISION]: must finish before the server proceeds (e.g. cancellable action).
 *   Delivered under a strict, documented contract; never silently dropped.
 *   Handlers run sequentially for a given event emission.
 */
enum class EventCategory {
    OBSERVATION,
    DECISION
}
