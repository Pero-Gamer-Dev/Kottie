package io.kottie.api.lifecycle

/**
 * Lifecycle states for plugins and the runtime.
 */
enum class LifecycleState {
    DISCOVERED,
    RESOLVING,
    STARTING,
    RUNNING,
    FAILED,
    STOPPING,
    STOPPED
}
