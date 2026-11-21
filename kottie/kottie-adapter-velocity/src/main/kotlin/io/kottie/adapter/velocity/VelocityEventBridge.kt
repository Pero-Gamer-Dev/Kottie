package io.kottie.adapter.velocity

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
 * Bridges Velocity Login/Disconnect (host-side) into Kottie player events.
 *
 * Host registers Velocity listeners and forwards:
 * ```
 * // on LoginEvent success / PostLoginEvent
 * bridge.onPlayerLogin(player.uniqueId, player.username)
 * // on DisconnectEvent
 * bridge.onPlayerDisconnect(player.uniqueId, player.username)
 * ```
 *
 * Optionally notifies cluster peers via a callback for cross-proxy tracking.
 */
class VelocityEventBridge(
    private val eventBus: EventBus,
    private val entityStore: EntityStore,
    private val scope: CoroutineScope,
    private val onLoginNotify: (suspend (UUID, String) -> Unit)? = null,
    private val onDisconnectNotify: (suspend (UUID, String) -> Unit)? = null
) {
    private val log = LoggerFactory.getLogger(VelocityEventBridge::class.java)
    private val playerEntities = ConcurrentHashMap<UUID, Entity>()

    fun onPlayerLogin(playerId: UUID, name: String) {
        val entity = try {
            entityStore.create(playerId)
        } catch (_: IllegalStateException) {
            entityStore.get(playerId) ?: entityStore.create(UUID.randomUUID())
        }
        playerEntities[playerId] = entity
        scope.launch {
            try {
                eventBus.emit(PlayerJoinEvent(playerId, name, entity))
                onLoginNotify?.invoke(playerId, name)
            } catch (t: Throwable) {
                log.error("Failed login bridge for {}", name, t)
            }
        }
    }

    fun onPlayerDisconnect(playerId: UUID, name: String) {
        scope.launch {
            try {
                eventBus.emit(PlayerQuitEvent(playerId, name))
                onDisconnectNotify?.invoke(playerId, name)
            } catch (t: Throwable) {
                log.error("Failed disconnect bridge for {}", name, t)
            } finally {
                playerEntities.remove(playerId)?.let { entityStore.remove(it) }
            }
        }
    }

    fun entityFor(playerId: UUID): Entity? = playerEntities[playerId]

    fun trackedPlayers(): Set<UUID> = playerEntities.keys.toSet()

    fun clear() {
        playerEntities.keys.toList().forEach { id ->
            playerEntities.remove(id)?.let { entityStore.remove(it) }
        }
    }
}
