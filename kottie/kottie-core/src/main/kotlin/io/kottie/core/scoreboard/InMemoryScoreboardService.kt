package io.kottie.core.scoreboard

import io.kottie.api.scoreboard.ScoreEntry
import io.kottie.api.scoreboard.ScoreboardObjective
import io.kottie.api.scoreboard.ScoreboardService
import io.kottie.api.scoreboard.ScoreboardSlot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryScoreboardService : ScoreboardService {

    private class Obj(
        override val name: String,
        override var displayName: String
    ) : ScoreboardObjective {
        private val scores = ConcurrentHashMap<String, Int>()
        override fun setScore(entry: String, score: Int) { scores[entry] = score }
        override fun getScore(entry: String): Int? = scores[entry]
        override fun removeScore(entry: String) { scores.remove(entry) }
        override fun entries(): List<ScoreEntry> =
            scores.map { ScoreEntry(it.key, it.value) }.sortedByDescending { it.score }
    }

    private val objectives = ConcurrentHashMap<String, Obj>()
    private val shown = ConcurrentHashMap<UUID, MutableMap<ScoreboardSlot, String>>()

    override fun createObjective(name: String, displayName: String): ScoreboardObjective {
        val obj = Obj(name, displayName)
        objectives[name] = obj
        return obj
    }

    override fun getObjective(name: String): ScoreboardObjective? = objectives[name]

    override fun removeObjective(name: String): Boolean {
        shown.values.forEach { slots ->
            slots.entries.removeIf { it.value == name }
        }
        return objectives.remove(name) != null
    }

    override fun show(playerId: UUID, objectiveName: String, slot: ScoreboardSlot) {
        require(objectives.containsKey(objectiveName)) { "unknown objective $objectiveName" }
        shown.computeIfAbsent(playerId) { ConcurrentHashMap() }[slot] = objectiveName
    }

    override fun clear(playerId: UUID, slot: ScoreboardSlot) {
        shown[playerId]?.remove(slot)
    }

    fun shownObjective(playerId: UUID, slot: ScoreboardSlot): String? =
        shown[playerId]?.get(slot)
}
