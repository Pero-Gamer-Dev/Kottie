package io.kottie.api.bossbar

import java.util.UUID

enum class BossBarColor {
    PINK, BLUE, RED, GREEN, YELLOW, PURPLE, WHITE
}

enum class BossBarStyle {
    SOLID,
    SEGMENTED_6,
    SEGMENTED_10,
    SEGMENTED_12,
    SEGMENTED_20
}

/**
 * Boss bar handle. Progress is 0.0..1.0.
 */
interface BossBar {
    val id: UUID
    var title: String
    var progress: Double
    var color: BossBarColor
    var style: BossBarStyle
    var visible: Boolean
    fun addViewer(playerId: UUID)
    fun removeViewer(playerId: UUID)
    fun viewers(): Set<UUID>
}

interface BossBarService {
    fun create(
        title: String,
        progress: Double = 1.0,
        color: BossBarColor = BossBarColor.PURPLE,
        style: BossBarStyle = BossBarStyle.SOLID
    ): BossBar

    fun get(id: UUID): BossBar?
    fun remove(id: UUID): Boolean
    fun all(): Collection<BossBar>
}
