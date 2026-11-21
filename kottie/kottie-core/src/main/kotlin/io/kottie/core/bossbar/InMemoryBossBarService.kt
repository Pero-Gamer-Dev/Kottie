package io.kottie.core.bossbar

import io.kottie.api.bossbar.BossBar
import io.kottie.api.bossbar.BossBarColor
import io.kottie.api.bossbar.BossBarService
import io.kottie.api.bossbar.BossBarStyle
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryBossBarService : BossBarService {

    private class Bar(
        override val id: UUID,
        override var title: String,
        progress: Double,
        override var color: BossBarColor,
        override var style: BossBarStyle
    ) : BossBar {
        override var progress: Double = progress.coerceIn(0.0, 1.0)
            set(value) { field = value.coerceIn(0.0, 1.0) }
        override var visible: Boolean = true
        private val viewers = ConcurrentHashMap.newKeySet<UUID>()
        override fun addViewer(playerId: UUID) { viewers.add(playerId) }
        override fun removeViewer(playerId: UUID) { viewers.remove(playerId) }
        override fun viewers(): Set<UUID> = viewers.toSet()
    }

    private val bars = ConcurrentHashMap<UUID, Bar>()

    override fun create(
        title: String,
        progress: Double,
        color: BossBarColor,
        style: BossBarStyle
    ): BossBar {
        val bar = Bar(UUID.randomUUID(), title, progress, color, style)
        bars[bar.id] = bar
        return bar
    }

    override fun get(id: UUID): BossBar? = bars[id]

    override fun remove(id: UUID): Boolean = bars.remove(id) != null

    override fun all(): Collection<BossBar> = bars.values.toList()
}
