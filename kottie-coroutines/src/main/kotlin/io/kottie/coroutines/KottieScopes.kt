package io.kottie.coroutines

import io.kottie.api.scheduler.ServerScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Creates a supervised scope owned by a plugin/runtime.
 * Cancel the scope on disable to prevent leaks.
 */
fun createPluginScope(
    name: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): CoroutineScope {
    return CoroutineScope(SupervisorJob() + dispatcher + kotlinx.coroutines.CoroutineName(name))
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
 * Tracks whether a scope has been cancelled to support diagnostics.
 */
class CancellableScope(private val scope: CoroutineScope) : CoroutineScope by scope {
    private val cancelled = AtomicBoolean(false)

    fun cancelSafely(reason: String? = null) {
        if (cancelled.compareAndSet(false, true)) {
            scope.cancel(kotlinx.coroutines.CancellationException(reason ?: "cancelled"))
        }
    }

    val isCancelled: Boolean get() = cancelled.get()
}
