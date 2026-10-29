package io.kottie.api.event

/**
 * Handle returned by [EventBus.subscribe]. Closing cancels the subscription.
 */
interface Subscription : AutoCloseable {
    val isClosed: Boolean
    override fun close()
}
