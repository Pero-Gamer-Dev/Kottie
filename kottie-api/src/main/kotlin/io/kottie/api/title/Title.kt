package io.kottie.api.title

import java.util.UUID

/**
 * Title / subtitle / times. Durations in ticks (20 ticks ≈ 1s).
 */
data class TitleTimes(
    val fadeInTicks: Int = 10,
    val stayTicks: Int = 70,
    val fadeOutTicks: Int = 20
)

data class TitleSpec(
    val title: String,
    val subtitle: String = "",
    val times: TitleTimes = TitleTimes()
)

interface TitleService {
    fun show(playerId: UUID, spec: TitleSpec)
    fun clear(playerId: UUID)
    fun reset(playerId: UUID) = clear(playerId)
}
