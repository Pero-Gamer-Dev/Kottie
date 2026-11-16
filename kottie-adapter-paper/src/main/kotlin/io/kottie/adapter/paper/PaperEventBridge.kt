package io.kottie.adapter.paper

import io.kottie.api.ecs.Entity
import io.kottie.api.ecs.EntityStore
import io.kottie.api.event.EventBus
import io.kottie.api.event.player.PlayerJoinEvent
import io.kottie.api.event.player.PlayerQuitEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges host (Paper/Bukkit) player join/quit into Kottie observation events.
 *
 * The host plugin should call [onPlayerJoin] / [onPlayerQuit] from its Bukkit
 * listeners, for example:
 *
 * ```
 * @EventHandler
 * fun onJoin(e: org.bukkit.event.player.PlayerJoinEvent) {
 *     bridge.onPlayerJoin(e.player.uniqueId, e.player.name)
 * }
 * ```
 *
 * No compile-time dependency on Bukkit is required.
 */
class PaperEventBridge(
    private val eventBus: EventBus,
    private val entityStore: EntityStore,
    private val scope: CoroutineScope
) {
    private val log = LoggerFactory.getLogger(PaperEventBridge::class.java)
    private val playerEntities = ConcurrentHashMap<UUID, Entity>()

    /**
     * Called by the host when a player joins.
     * Creates a Kottie entity keyed by the player UUID and emits [PlayerJoinEvent].
     */
    fun onPlayerJoin(playerId: UUID, name: String) {
        val entity = try {
            entityStore.create(playerId)
        } catch (_: IllegalStateException) {
            // Entity may already exist after a partial restart; reuse
            entityStore.get(playerId) ?: entityStore.create(UUID.randomUUID()).also {
                log.warn("Could not bind entity to playerId={}; allocated ephemeral entity", playerId)
            }
        }
        playerEntities[playerId] = entity
        scope.launch {
            try {
                eventBus.emit(PlayerJoinEvent(playerId, name, entity))
            } catch (t: Throwable) {
                log.error("Failed to emit PlayerJoinEvent for {}", name, t)
            }
        }
    }

    /**
     * Called by the host when a player quits.
     * Emits [PlayerQuitEvent] and removes the Kottie entity.
     */
    fun onPlayerQuit(playerId: UUID, name: String) {
        scope.launch {
            try {
                eventBus.emit(PlayerQuitEvent(playerId, name))
            } catch (t: Throwable) {
                log.error("Failed to emit PlayerQuitEvent for {}", name, t)
            } finally {
                playerEntities.remove(playerId)?.let { entityStore.remove(it) }
            }
        }
    }

    fun entityFor(playerId: UUID): Entity? = playerEntities[playerId]

    fun clear() {
        playerEntities.keys.toList().forEach { id ->
            playerEntities.remove(id)?.let { entityStore.remove(it) }
        }
    }
}
