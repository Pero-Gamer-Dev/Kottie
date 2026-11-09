package example

import io.kottie.api.Kottie
import io.kottie.api.ecs.components.ManaComponent
import io.kottie.api.event.player.PlayerJoinEvent
import io.kottie.api.event.subscribe
import io.kottie.api.lifecycle.LifecycleState
import io.kottie.api.lifecycle.PluginLifecycle
import kotlinx.coroutines.CoroutineScope
import java.time.Duration

/**
 * Example plugin demonstrating the 0.2.0 lifecycle pattern.
 * Collector is launched inside the plugin-owned scope and cancelled on disable.
 */
class MyCustomPlugin(
    private val kottie: Kottie
) : PluginLifecycle {

    override val state: LifecycleState
        get() = _state
    private var _state = LifecycleState.DISCOVERED

    override val scope: CoroutineScope
        get() = kottie.lifecycle.scope

    private var joinSubscription: AutoCloseable? = null

    override suspend fun onEnable() {
        _state = LifecycleState.STARTING

        joinSubscription = kottie.eventBus.subscribe<PlayerJoinEvent> { event ->
            kottie.scheduler.onServerThread {
                event.entity.set(ManaComponent.KEY, ManaComponent(maxMana = 100, currentMana = 100))
            }
            println("Successfully initialized custom network profile for ${event.name}")
        }

        _state = LifecycleState.RUNNING
    }

    override suspend fun onDisable(timeout: Duration) {
        _state = LifecycleState.STOPPING
        joinSubscription?.close()
        joinSubscription = null
        _state = LifecycleState.STOPPED
    }
}
