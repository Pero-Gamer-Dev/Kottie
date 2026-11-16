package io.kottie.ecs

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey
import io.kottie.api.ecs.Entity
import io.kottie.api.ecs.EntityListener
import io.kottie.api.ecs.EntityQuery
import io.kottie.api.ecs.EntityStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory entity store with a component membership index for queries.
 *
 * Index maps component key id → set of entity UUIDs that currently hold it.
 * [query] intersects those sets instead of scanning every entity when possible.
 */
class DefaultEntityStore : EntityStore {

    private val entities = ConcurrentHashMap<UUID, SimpleEntity>()
    private val listeners = CopyOnWriteArrayList<EntityListener>()
    /** component key id → entity ids */
    private val index = ConcurrentHashMap<String, MutableSet<UUID>>()

    private fun currentListeners(): List<EntityListener> = listeners.toList()

    private val indexingListener = object : EntityListener {
        override fun onComponentSet(entity: Entity, key: ComponentKey<*>, component: Component) {
            index.computeIfAbsent(key.id) { ConcurrentHashMap.newKeySet() }.add(entity.uniqueId)
        }

        override fun onComponentRemoved(entity: Entity, key: ComponentKey<*>, previous: Component?) {
            index[key.id]?.remove(entity.uniqueId)
        }

        override fun onEntityRemoved(entity: Entity) {
            // SimpleEntity clears components; also scrub index
            index.values.forEach { it.remove(entity.uniqueId) }
        }
    }

    init {
        listeners.add(indexingListener)
    }

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
            }
        }
        return entity
    }

    override fun remove(entity: Entity): Boolean {
        val removed = entities.remove(entity.uniqueId) ?: return false
        removed.clearComponents()
        index.values.forEach { it.remove(removed.uniqueId) }
        listeners.forEach { listener ->
            try {
                listener.onEntityRemoved(removed)
            } catch (_: Throwable) {
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
        if (listener === indexingListener) return
        listeners.remove(listener)
    }

    override fun query(query: EntityQuery): Collection<Entity> {
        // Start from the smallest required key set
        val requiredLists = query.required.map { key ->
            index[key.id]?.toSet().orEmpty()
        }
        if (requiredLists.any { it.isEmpty() }) return emptyList()

        var candidateIds = requiredLists.minBy { it.size }
        for (set in requiredLists) {
            candidateIds = candidateIds.intersect(set)
            if (candidateIds.isEmpty()) return emptyList()
        }

        return candidateIds.mapNotNull { id -> entities[id] }.filter { entity ->
            query.required.all { entity.has(it) } &&
                query.excluded.none { entity.has(it) }
        }
    }
}
