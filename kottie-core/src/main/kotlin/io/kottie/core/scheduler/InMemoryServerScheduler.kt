package io.kottie.core.scheduler

import io.kottie.api.scheduler.ServerScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * Development / test scheduler that simulates a single "server thread".
 * Production adapters replace this with the real Paper/Bukkit scheduler.
 */
class InMemoryServerScheduler : ServerScheduler {

    private val serverThreadFactory = object : ThreadFactory {
        private val counter = AtomicInteger()
        override fun newThread(r: Runnable): Thread {
            return Thread(r, "kottie-server-thread-${counter.getAndIncrement()}").apply {
                isDaemon = true
            }
        }
    }

    private val serverExecutor = Executors.newSingleThreadExecutor(serverThreadFactory)
    private val serverDispatcher = serverExecutor.asCoroutineDispatcher()

    private val ioExecutor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(2),
        object : ThreadFactory {
            private val counter = AtomicInteger()
            override fun newThread(r: Runnable): Thread {
                return Thread(r, "kottie-io-${counter.getAndIncrement()}").apply { isDaemon = true }
            }
        }
    )
    private val ioDispatcher = ioExecutor.asCoroutineDispatcher()

    private val computeDispatcher = Dispatchers.Default

    @Volatile
    private var serverThread: Thread? = null

    init {
        // Capture the actual server thread identity
        serverExecutor.submit {
            serverThread = Thread.currentThread()
        }.get()
    }

    override suspend fun <T> onServerThread(block: () -> T): T =
        withContext(serverDispatcher) { block() }

    override fun executeOnServerThread(block: () -> Unit) {
        serverExecutor.execute(block)
    }

    override fun isServerThread(): Boolean =
        Thread.currentThread() === serverThread

    override fun ioDispatcher(): CoroutineDispatcher = ioDispatcher

    override fun computeDispatcher(): CoroutineDispatcher = computeDispatcher

    fun shutdown() {
        serverExecutor.shutdownNow()
        ioExecutor.shutdownNow()
    }
}
