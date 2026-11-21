package io.kottie.api.ecs

/**
 * Declarative query over entities that own a set of required components.
 *
 * Matching is structural (has all keys), not archetype-stable across mutations
 * unless the store maintains an index (see [EntityStore.query]).
 */
data class EntityQuery(
    val required: Set<ComponentKey<*>>,
    val excluded: Set<ComponentKey<*>> = emptySet()
) {
    init {
        require(required.isNotEmpty()) { "EntityQuery requires at least one component key" }
    }

    companion object {
        fun allOf(vararg keys: ComponentKey<*>): EntityQuery =
            EntityQuery(required = keys.toSet())

        fun allOf(keys: Collection<ComponentKey<*>>): EntityQuery =
            EntityQuery(required = keys.toSet())
    }
}

/**
 * Tickable system that processes entities matching a query.
 * Hosts should call [tick] from their scheduler (server thread or async, as appropriate).
 */
fun interface EntitySystem {
    fun tick(entities: Collection<Entity>, deltaMs: Long)
}
