package io.kottie.adapter.velocity

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
 * Proxy-oriented scheduler. On Velocity, wire [proxyExecutor] to the proxy's
 * main/event thread executor (e.g. schedule on the proxy event loop).
 */
class VelocityServerScheduler(
    private val proxyExecutor: Executor,
    private val isProxyThread: () -> Boolean,
    ioThreads: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
) : ServerScheduler {

    private val proxyThreadRef = AtomicReference<Thread?>(null)

    private val ioExecutor = Executors.newFixedThreadPool(
        ioThreads,
        object : ThreadFactory {
            private val n = AtomicInteger()
            override fun newThread(r: Runnable): Thread =
                Thread(r, "kottie-velocity-io-${n.getAndIncrement()}").apply { isDaemon = true }
        }
    )
    private val ioDispatcher = ioExecutor.asCoroutineDispatcher()

    init {
        proxyExecutor.execute {
            proxyThreadRef.compareAndSet(null, Thread.currentThread())
        }
    }

    override suspend fun <T> onServerThread(block: () -> T): T {
        if (isServerThread()) return block()
        return suspendCancellableCoroutine { cont ->
            proxyExecutor.execute {
                try {
                    cont.resume(block())
                } catch (t: Throwable) {
                    cont.resumeWithException(t)
                }
            }
        }
    }

    override fun executeOnServerThread(block: () -> Unit) {
        if (isServerThread()) block()
        else proxyExecutor.execute(block)
    }

    override fun isServerThread(): Boolean {
        val captured = proxyThreadRef.get()
        if (captured != null && Thread.currentThread() === captured) return true
        return try {
            isProxyThread()
        } catch (_: Throwable) {
            false
        }
    }

    override fun ioDispatcher(): CoroutineDispatcher = ioDispatcher

    override fun computeDispatcher(): CoroutineDispatcher = Dispatchers.Default

    fun shutdown() {
        ioExecutor.shutdownNow()
    }
}
