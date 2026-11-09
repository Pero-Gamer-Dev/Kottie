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
    /**
     * Create a new Kottie-managed entity and fire creation hooks.
     */
    fun create(): Entity

    /**
     * Create with a specific UUID (e.g. matching a player id).
     */
    fun create(uniqueId: java.util.UUID): Entity

    /**
     * Remove an entity, strip components, and fire removal hooks.
     * Returns true if the entity was present.
     */
    fun remove(entity: Entity): Boolean

    /**
     * Lookup by id.
     */
    fun get(uniqueId: java.util.UUID): Entity?

    /**
     * Current entities (snapshot).
     */
    fun all(): Collection<Entity>

    fun addListener(listener: EntityListener)
    fun removeListener(listener: EntityListener)
}
