package io.kottie.ecs

import io.kottie.api.ecs.Entity
import io.kottie.api.ecs.EntityListener
import io.kottie.api.ecs.EntityStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Default in-memory entity store with lifecycle hooks.
 */
class DefaultEntityStore : EntityStore {

    private val entities = ConcurrentHashMap<UUID, SimpleEntity>()
    private val listeners = CopyOnWriteArrayList<EntityListener>()

    private fun currentListeners(): List<EntityListener> = listeners.toList()

    override fun create(): Entity = create(UUID.randomUUID())

    override fun create(uniqueId: UUID): Entity {
        val entity = SimpleEntity(uniqueId) { currentListeners() }
        val previous = entities.putIfAbsent(uniqueId, entity)
        if (previous != null) {
            throw IllegalStateException("Entity already exists: $uniqueId")
        }
        listeners.forEach { listener ->
            try {
                listener.onEntityCreated(entity)
            } catch (_: Throwable) {
                // isolation
            }
        }
        return entity
    }

    override fun remove(entity: Entity): Boolean {
        val removed = entities.remove(entity.uniqueId) ?: return false
        removed.clearComponents()
        listeners.forEach { listener ->
            try {
                listener.onEntityRemoved(removed)
            } catch (_: Throwable) {
                // isolation
            }
        }
        return true
    }

    override fun get(uniqueId: UUID): Entity? = entities[uniqueId]

    override fun all(): Collection<Entity> = entities.values.toList()

    override fun addListener(listener: EntityListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: EntityListener) {
        listeners.remove(listener)
    }
}
