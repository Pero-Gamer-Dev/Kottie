package io.kottie.core.scheduler

import io.kottie.api.scheduler.RegionScheduler
import io.kottie.api.scheduler.ServerScheduler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Region scheduler that falls back to the server thread (Paper) or a global
 * executor (tests). Hosts on Folia should supply a real region implementation.
 */
class FallbackRegionScheduler(
    private val serverScheduler: ServerScheduler
) : RegionScheduler {

    private val delayed: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "kottie-region-delay").apply { isDaemon = true }
        }

    override fun execute(worldName: String, blockX: Int, blockZ: Int, task: () -> Unit) {
        serverScheduler.executeOnServerThread(task)
    }

    override fun executeGlobal(task: () -> Unit) {
        serverScheduler.executeOnServerThread(task)
    }

    override fun runDelayed(
        worldName: String,
        blockX: Int,
        blockZ: Int,
        delayTicks: Long,
        task: () -> Unit
    ) {
        val ms = (delayTicks.coerceAtLeast(0) * 50)
        delayed.schedule({
            serverScheduler.executeOnServerThread(task)
        }, ms, TimeUnit.MILLISECONDS)
    }

    fun shutdown() {
        delayed.shutdownNow()
    }
}
