package io.kottie.api.scheduler

/**
 * Explicit scheduler abstraction separating I/O, compute, and server-affine work.
 *
 * Never assume a server object becomes thread-safe merely because it is used
 * inside a coroutine. Use [onServerThread] / [executeOnServerThread] for any
 * mutation of world, entity, inventory, or other server-thread-affine state.
 */
interface ServerScheduler {
    /**
     * Execute a block that must run on the server's primary thread.
     * Suspends the caller until the block completes.
     */
    suspend fun <T> onServerThread(block: () -> T): T

    /**
     * Fire-and-forget execution on the server thread.
     */
    fun executeOnServerThread(block: () -> Unit)

    /**
     * True when the current thread is the server's primary (game) thread.
     */
    fun isServerThread(): Boolean

    /**
     * Bounded dispatcher suitable for blocking I/O.
     * Prefer structured concurrency helpers from kottie-coroutines.
     */
    fun ioDispatcher(): kotlinx.coroutines.CoroutineDispatcher

    /**
     * Bounded dispatcher suitable for pure CPU work.
     */
    fun computeDispatcher(): kotlinx.coroutines.CoroutineDispatcher
}
