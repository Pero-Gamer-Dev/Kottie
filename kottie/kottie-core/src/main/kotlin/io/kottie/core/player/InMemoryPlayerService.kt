package io.kottie.core.player

import io.kottie.api.player.GameMode
import io.kottie.api.player.PlayerService
import io.kottie.api.player.PlayerView
import io.kottie.api.world.LocationSnapshot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class MutablePlayerView(
    override val uniqueId: UUID,
    override var name: String,
    override var displayName: String = name,
    override var location: LocationSnapshot,
    override var health: Double = 20.0,
    override var maxHealth: Double = 20.0,
    override var level: Int = 0,
    override var gameMode: GameMode = GameMode.SURVIVAL,
    override var isOnline: Boolean = true
) : PlayerView

class InMemoryPlayerService : PlayerService {
    private val byId = ConcurrentHashMap<UUID, MutablePlayerView>()

    override fun onlinePlayers(): Collection<PlayerView> =
        byId.values.filter { it.isOnline }

    override fun get(uniqueId: UUID): PlayerView? = byId[uniqueId]

    override fun getByName(name: String): PlayerView? =
        byId.values.find { it.name.equals(name, ignoreCase = true) }

    override fun count(): Int = byId.values.count { it.isOnline }

    override fun teleport(playerId: UUID, location: LocationSnapshot): Boolean {
        val p = byId[playerId] ?: return false
        if (!p.isOnline) return false
        p.location = location
        return true
    }

    override fun update(view: PlayerView) {
        val existing = byId[view.uniqueId]
        if (existing != null && view !is MutablePlayerView) {
            existing.name = view.name
            existing.displayName = view.displayName
            existing.location = view.location
            existing.health = view.health
            existing.maxHealth = view.maxHealth
            existing.level = view.level
            existing.gameMode = view.gameMode
            existing.isOnline = view.isOnline
        } else if (view is MutablePlayerView) {
            byId[view.uniqueId] = view
        } else {
            byId[view.uniqueId] = MutablePlayerView(
                uniqueId = view.uniqueId,
                name = view.name,
                displayName = view.displayName,
                location = view.location,
                health = view.health,
                maxHealth = view.maxHealth,
                level = view.level,
                gameMode = view.gameMode,
                isOnline = view.isOnline
            )
        }
    }

    override fun remove(playerId: UUID) {
        byId.remove(playerId)
    }
}
