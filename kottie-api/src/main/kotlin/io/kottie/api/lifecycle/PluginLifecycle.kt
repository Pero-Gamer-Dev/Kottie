package io.kottie.api.lifecycle

import kotlinx.coroutines.CoroutineScope

/**
 * Owns the lifecycle and coroutine scope for a plugin or the core runtime.
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
     */
    suspend fun onDisable()
}
