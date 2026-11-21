package io.kottie.api.player

import io.kottie.api.inventory.InventorySnapshot
import io.kottie.api.world.LocationSnapshot
import java.util.UUID

/**
 * Online player view — metadata only, not a live Bukkit Player reference.
 */
interface PlayerView {
    val uniqueId: UUID
    val name: String
    val displayName: String
    val location: LocationSnapshot
    val health: Double
    val maxHealth: Double
    val level: Int
    val gameMode: GameMode
    val isOnline: Boolean
}

enum class GameMode {
    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR
}

/**
 * Platform player directory. Hosts sync from Bukkit Player join/quit.
 */
interface PlayerService {
    fun onlinePlayers(): Collection<PlayerView>
    fun get(uniqueId: UUID): PlayerView?
    fun getByName(name: String): PlayerView?
    fun count(): Int

    /**
     * Teleport request — host applies on the correct region/server thread.
     * @return false if player offline or host rejected
     */
    fun teleport(playerId: UUID, location: LocationSnapshot): Boolean

    /**
     * Update stored view (used by hosts and in-memory test service).
     */
    fun update(view: PlayerView)

    fun remove(playerId: UUID)
}
