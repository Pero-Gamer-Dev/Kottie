package io.kottie.api.ecs

/**
 * Lifecycle hooks for entities managed by an [EntityStore].
 */
interface EntityListener {
    fun onEntityCreated(entity: Entity) {}
    fun onEntityRemoved(entity: Entity) {}
    fun onComponentSet(entity: Entity, key: ComponentKey<*>, component: Component) {}
    fun onComponentRemoved(entity: Entity, key: ComponentKey<*>, previous: Component?) {}
}

/**
 * Store that owns entity identity and lifecycle.
 * Does not replace the underlying Minecraft server entity model.
 */
interface EntityStore {
    fun create(): Entity
    fun create(uniqueId: java.util.UUID): Entity
    fun remove(entity: Entity): Boolean
    fun get(uniqueId: java.util.UUID): Entity?
    fun all(): Collection<Entity>
    fun addListener(listener: EntityListener)
    fun removeListener(listener: EntityListener)

    /**
     * Return entities that have all [EntityQuery.required] components and none of
     * [EntityQuery.excluded]. Default implementation scans [all]; indexed stores
     * may override for better performance.
     */
    fun query(query: EntityQuery): Collection<Entity> {
        return all().filter { entity ->
            query.required.all { entity.has(it) } &&
                query.excluded.none { entity.has(it) }
        }
    }

    /**
     * Convenience: entities that have every listed key.
     */
    fun withAll(vararg keys: ComponentKey<*>): Collection<Entity> =
        query(EntityQuery.allOf(*keys))
}
