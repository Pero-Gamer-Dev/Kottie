package io.kottie.server

import io.kottie.api.scheduler.ServerScheduler

/**
 * Contract that a concrete server integration (Paper, etc.) must implement.
 * The public API never depends on a specific server implementation.
 */
interface ServerAdapter {
    val scheduler: ServerScheduler
    val serverName: String
    val supportedVersions: Set<String>

    /**
     * Called once during runtime bootstrap.
     */
    fun initialize()

    /**
     * Called during graceful shutdown.
     */
    fun shutdown()
}
