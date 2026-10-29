package io.kottie.api

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

    companion object {
        @Volatile
        private var instance: Kottie? = null

        fun get(): Kottie =
            instance ?: error("Kottie runtime has not been initialized")

        /** Bootstrap only – called by the runtime. */
        fun setInstance(kottie: Kottie) {
            instance = kottie
        }

        /** Bootstrap only – called by the runtime on shutdown. */
        fun clearInstance() {
            instance = null
        }
    }
}
