package io.kottie.core.world

import io.kottie.api.world.LocationSnapshot
import io.kottie.api.world.WorldEnvironment
import io.kottie.api.world.WorldService
import io.kottie.api.world.WorldView
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class SimpleWorldView(
    override val name: String,
    override val uid: UUID = UUID.randomUUID(),
    override val environment: WorldEnvironment = WorldEnvironment.OVERWORLD,
    override var playerCount: Int = 0,
    override var spawn: LocationSnapshot = LocationSnapshot(name, 0.0, 64.0, 0.0)
) : WorldView

class InMemoryWorldService(
    defaultName: String = "world"
) : WorldService {
    private val byName = ConcurrentHashMap<String, SimpleWorldView>()
    private val byUid = ConcurrentHashMap<UUID, SimpleWorldView>()

    init {
        val w = SimpleWorldView(defaultName)
        byName[w.name.lowercase()] = w
        byUid[w.uid] = w
    }

    fun add(world: SimpleWorldView) {
        byName[world.name.lowercase()] = world
        byUid[world.uid] = world
    }

    override fun worlds(): Collection<WorldView> = byName.values.toList()

    override fun get(name: String): WorldView? = byName[name.lowercase()]

    override fun getByUid(uid: UUID): WorldView? = byUid[uid]

    override fun defaultWorld(): WorldView? = byName.values.firstOrNull()
}
