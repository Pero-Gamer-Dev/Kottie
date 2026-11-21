package io.kottie.api.lifecycle

import kotlinx.coroutines.CoroutineScope
import java.time.Duration

/**
 * Owns the lifecycle and coroutine scope for a plugin or the core runtime.
 *
 * Contract:
 * - [onEnable] must transition DISCOVERED/RESOLVING → STARTING → RUNNING (or FAILED).
 * - [onDisable] must transition RUNNING → STOPPING → STOPPED (or FAILED).
 * - All long-lived work must be launched in [scope] so it is cancelled on disable.
 * - Shutdown is expected to complete within a timeout; unfinished work is reported.
 */
interface PluginLifecycle {
    val state: LifecycleState
    val scope: CoroutineScope

    /**
     * Called when the plugin should start. Implementations must transition
     * state and launch any long-lived work inside [scope].
     */
    suspend fun onEnable()

    /**
     * Called when the plugin should stop. Implementations must cancel work,
     * close resources, and transition to STOPPED (or FAILED).
     *
     * @param timeout maximum time allowed for graceful cancellation and resource release.
     *                Unfinished tasks after the timeout should be reported via diagnostics.
     */
    suspend fun onDisable(timeout: Duration = Duration.ofSeconds(10))
}
