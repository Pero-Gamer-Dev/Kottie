package io.kottie.api.scheduler

/**
 * Folia-oriented region scheduling. On non-Folia hosts, implementations may
 * fall back to [ServerScheduler.executeOnServerThread].
 *
 * Coordinates are block positions in a world identified by [worldName].
 */
interface RegionScheduler {
    fun execute(worldName: String, blockX: Int, blockZ: Int, task: () -> Unit)

    fun executeGlobal(task: () -> Unit)

    /**
     * Run after [delayTicks] on the region that owns the location.
     */
    fun runDelayed(worldName: String, blockX: Int, blockZ: Int, delayTicks: Long, task: () -> Unit)
}
