package io.kottie.api.scoreboard

import java.util.UUID

enum class ScoreboardSlot {
    SIDEBAR,
    PLAYER_LIST,
    BELOW_NAME
}

data class ScoreEntry(
    val name: String,
    val score: Int
)

/**
 * Platform-neutral scoreboard objective.
 * Hosts map to Bukkit Scoreboard / Adventure.
 */
interface ScoreboardObjective {
    val name: String
    var displayName: String
    fun setScore(entry: String, score: Int)
    fun getScore(entry: String): Int?
    fun removeScore(entry: String)
    fun entries(): List<ScoreEntry>
}

interface ScoreboardService {
    fun createObjective(name: String, displayName: String = name): ScoreboardObjective
    fun getObjective(name: String): ScoreboardObjective?
    fun removeObjective(name: String): Boolean
    fun show(playerId: UUID, objectiveName: String, slot: ScoreboardSlot = ScoreboardSlot.SIDEBAR)
    fun clear(playerId: UUID, slot: ScoreboardSlot = ScoreboardSlot.SIDEBAR)
}
