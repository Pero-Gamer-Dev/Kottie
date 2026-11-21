package io.kottie.core.title

import io.kottie.api.title.TitleService
import io.kottie.api.title.TitleSpec
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class RecordingTitleService : TitleService {
    data class Shown(val playerId: UUID, val spec: TitleSpec)

    private val history = CopyOnWriteArrayList<Shown>()
    private val active = ConcurrentHashMap<UUID, TitleSpec>()

    val shown: List<Shown> get() = history.toList()

    override fun show(playerId: UUID, spec: TitleSpec) {
        active[playerId] = spec
        history.add(Shown(playerId, spec))
    }

    override fun clear(playerId: UUID) {
        active.remove(playerId)
    }

    fun active(playerId: UUID): TitleSpec? = active[playerId]
}
