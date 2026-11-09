package io.kottie.coroutines

import io.kottie.api.scheduler.ServerScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Creates a supervised scope owned by a plugin/runtime.
 * Cancel the scope on disable to prevent leaks.
 */
fun createPluginScope(
    name: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): CoroutineScope {
    return CoroutineScope(SupervisorJob() + dispatcher + CoroutineName(name))
}

/**
 * Helper that runs a block on the I/O dispatcher provided by the scheduler.
 */
suspend fun <T> ServerScheduler.io(block: suspend () -> T): T =
    withContext(ioDispatcher()) { block() }

/**
 * Helper that runs a block on the compute dispatcher.
 */
suspend fun <T> ServerScheduler.compute(block: suspend () -> T): T =
    withContext(computeDispatcher()) { block() }

/**
 * Tracks whether a scope has been cancelled and supports timed cancellation.
 */
class CancellableScope(
    private val scope: CoroutineScope
) : CoroutineScope by scope {

    private val cancelled = AtomicBoolean(false)

    val isCancelled: Boolean get() = cancelled.get()

    /**
     * Cancel the scope. Returns true if this call performed the cancellation.
     */
    fun cancelSafely(reason: String? = null): Boolean {
        if (cancelled.compareAndSet(false, true)) {
            scope.cancel(kotlinx.coroutines.CancellationException(reason ?: "cancelled"))
            return true
        }
        return false
    }

    /**
     * Cancel and wait up to [timeout] for the root job to complete.
     * Returns the list of still-active child jobs after the timeout (empty = clean).
     */
    suspend fun cancelAndJoinWithTimeout(timeout: Duration, reason: String? = null): List<Job> {
        cancelSafely(reason)
        val root = scope.coroutineContext[Job] ?: return emptyList()
        val joined = withTimeoutOrNull(timeout.toMillis()) {
            root.join()
            true
        }
        if (joined == true) return emptyList()
        return root.children.filter { it.isActive }.toList()
    }
}
