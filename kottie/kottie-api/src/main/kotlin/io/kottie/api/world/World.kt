package io.kottie.api.world

/**
 * Immutable block/world position snapshot — never a live Bukkit Location.
 */
data class LocationSnapshot(
    val worldName: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f
) {
    val blockX: Int get() = x.toInt().let { if (x < 0 && x != it.toDouble()) it - 1 else it }
    val blockY: Int get() = y.toInt().let { if (y < 0 && y != it.toDouble()) it - 1 else it }
    val blockZ: Int get() = z.toInt().let { if (z < 0 && z != it.toDouble()) it - 1 else it }
}

/**
 * Read-only world view for plugins. Hosts bridge to Bukkit World.
 */
interface WorldView {
    val name: String
    val uid: java.util.UUID
    val environment: WorldEnvironment
    val playerCount: Int
    val spawn: LocationSnapshot
}

enum class WorldEnvironment {
    OVERWORLD,
    NETHER,
    THE_END,
    CUSTOM
}

interface WorldService {
    fun worlds(): Collection<WorldView>
    fun get(name: String): WorldView?
    fun getByUid(uid: java.util.UUID): WorldView?
    fun defaultWorld(): WorldView?
}
