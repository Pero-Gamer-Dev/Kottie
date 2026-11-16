package example

import io.kottie.api.Kottie
import io.kottie.api.cluster.ClusterMessenger
import io.kottie.api.config.ConfigService
import io.kottie.api.ecs.EntityQuery
import io.kottie.api.ecs.components.CooldownComponent
import io.kottie.api.ecs.components.HealthComponent
import io.kottie.api.ecs.components.ManaComponent
import io.kottie.api.ecs.components.StatsComponent
import io.kottie.api.event.player.PlayerJoinEvent
import io.kottie.api.event.subscribe
import io.kottie.api.lifecycle.LifecycleState
import io.kottie.api.lifecycle.PluginLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * End-to-end sample (1.8):
 * - custom / built-in ECS components on join
 * - cooldown system tick loop
 * - optional config observe
 * - optional cross-server cluster publish
 *
 * Hosts inject [Kottie] plus optional [ClusterMessenger] / use [Kottie.config].
 */
class MyCustomPlugin(
    private val kottie: Kottie,
    private val clusterMessenger: ClusterMessenger? = null
) : PluginLifecycle {

    override val state: LifecycleState
        get() = _state
    private var _state = LifecycleState.DISCOVERED

    override val scope: CoroutineScope
        get() = kottie.lifecycle.scope

    private var joinSubscription: AutoCloseable? = null
    private var tickJob: Job? = null
    private var configJob: Job? = null

    override suspend fun onEnable() {
        _state = LifecycleState.STARTING

        // Register / document built-in keys (registry may already contain them)
        runCatching {
            kottie.componentRegistry.register(ManaComponent.KEY)
            kottie.componentRegistry.register(HealthComponent.KEY)
            kottie.componentRegistry.register(CooldownComponent.KEY)
            kottie.componentRegistry.register(StatsComponent.KEY)
        }

        joinSubscription = kottie.eventBus.subscribe<PlayerJoinEvent> { event ->
            kottie.scheduler.onServerThread {
                event.entity.set(ManaComponent.KEY, ManaComponent(maxMana = 100, currentMana = 100))
                event.entity.set(HealthComponent.KEY, HealthComponent(maxHealth = 20.0, currentHealth = 20.0))
                event.entity.set(
                    StatsComponent.KEY,
                    StatsComponent(base = mapOf("strength" to 10.0, "speed" to 1.0))
                )
                event.entity.set(
                    CooldownComponent.KEY,
                    CooldownComponent().trigger("welcome_burst", 3_000)
                )
            }

            // Optional cross-server notify
            clusterMessenger?.publish(
                topic = "player-join",
                payload = """{"name":"${event.name}","id":"${event.playerId}"}""".encodeToByteArray()
            )

            println("[example] initialized profile for ${event.name}")
        }

        // Cooldown tick system (~20 TPS style interval)
        tickJob = scope.launch {
            val query = EntityQuery.allOf(CooldownComponent.KEY)
            while (isActive) {
                delay(50)
                val entities = kottie.entityStore.query(query)
                kottie.scheduler.onServerThread {
                    entities.forEach { entity ->
                        val cd = entity.get(CooldownComponent.KEY) ?: return@forEach
                        entity.set(CooldownComponent.KEY, cd.tick(50))
                    }
                }
            }
        }

        // Config hot-observe (plugin-defined type registered by host ConfigService)
        configJob = scope.launch {
            runCatching {
                observeExampleConfig(kottie.config)
            }
        }

        _state = LifecycleState.RUNNING
    }

    override suspend fun onDisable(timeout: Duration) {
        _state = LifecycleState.STOPPING
        tickJob?.cancel()
        configJob?.cancel()
        joinSubscription?.close()
        joinSubscription = null
        tickJob = null
        configJob = null
        _state = LifecycleState.STOPPED
    }

    private suspend fun observeExampleConfig(config: ConfigService) {
        // Hosts bind ExamplePluginConfig via ConfigService before enable.
        runCatching {
            val flow = config.observe(ExamplePluginConfig::class.java)
            flow.collect { cfg ->
                println("[example] config reload: welcomeMessage=${cfg.welcomeMessage}, maxMana=${cfg.defaultMaxMana}")
            }
        }
    }
}

/**
 * Sample typed config section for hot-reload demos.
 * Validate in a ConfigValidator registered by the host.
 */
data class ExamplePluginConfig(
    val welcomeMessage: String = "Welcome!",
    val defaultMaxMana: Int = 100
)
