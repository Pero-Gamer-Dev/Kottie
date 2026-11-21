package io.kottie.api

import io.kottie.api.config.ConfigService
import io.kottie.api.ecs.ComponentRegistry
import io.kottie.api.ecs.EntityStore
import io.kottie.api.event.EventBus
import io.kottie.api.lifecycle.PluginLifecycle
import io.kottie.api.scheduler.ServerScheduler

/**
 * Main entry point facade for the Kottie platform.
 * Obtained from the runtime after successful bootstrap.
 */
interface Kottie {
    val eventBus: EventBus
    val scheduler: ServerScheduler
    val lifecycle: PluginLifecycle
    val entityStore: EntityStore
    val componentRegistry: ComponentRegistry
    val config: ConfigService

    companion object {
        @Volatile
        private var instance: Kottie? = null

        fun get(): Kottie =
            instance ?: error("Kottie runtime has not been initialized")

        fun setInstance(kottie: Kottie) {
            instance = kottie
        }

        fun clearInstance() {
            instance = null
        }
    }
}
