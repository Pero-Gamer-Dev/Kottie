package io.kottie.adapter.paper

import io.kottie.api.scheduler.ServerScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Server scheduler backed by a host-provided main-thread executor.
 *
 * On Paper/Bukkit, wire [mainThreadExecutor] to something equivalent to:
 * `Bukkit.getScheduler().runTask(plugin) { ... }` / the server primary thread.
 *
 * This class does not depend on the Paper API at compile time so the module
 * builds in CI without a Minecraft server JAR.
 */
class PaperServerScheduler(
    private val mainThreadExecutor: Executor,
    private val isPrimaryThread: () -> Boolean,
    ioThreads: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
) : ServerScheduler {

    private val serverThreadRef = AtomicReference<Thread?>(null)

    private val ioExecutor = Executors.newFixedThreadPool(
        ioThreads,
        object : ThreadFactory {
            private val n = AtomicInteger()
            override fun newThread(r: Runnable): Thread =
                Thread(r, "kottie-paper-io-${n.getAndIncrement()}").apply { isDaemon = true }
        }
    )
    private val ioDispatcher = ioExecutor.asCoroutineDispatcher()
    private val computeDispatcher = Dispatchers.Default

    init {
        // Best-effort capture of the primary thread identity
        mainThreadExecutor.execute {
            serverThreadRef.compareAndSet(null, Thread.currentThread())
        }
    }

    override suspend fun <T> onServerThread(block: () -> T): T {
        if (isServerThread()) return block()
        return suspendCancellableCoroutine { cont ->
            mainThreadExecutor.execute {
                try {
                    cont.resume(block())
                } catch (t: Throwable) {
                    cont.resumeWithException(t)
                }
            }
        }
    }

    override fun executeOnServerThread(block: () -> Unit) {
        if (isServerThread()) {
            block()
        } else {
            mainThreadExecutor.execute(block)
        }
    }

    override fun isServerThread(): Boolean {
        val captured = serverThreadRef.get()
        if (captured != null && Thread.currentThread() === captured) return true
        return try {
            isPrimaryThread()
        } catch (_: Throwable) {
            false
        }
    }

    override fun ioDispatcher(): CoroutineDispatcher = ioDispatcher

    override fun computeDispatcher(): CoroutineDispatcher = computeDispatcher

    fun shutdown() {
        ioExecutor.shutdownNow()
    }
}
