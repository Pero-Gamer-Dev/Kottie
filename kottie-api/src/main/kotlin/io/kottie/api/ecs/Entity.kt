package io.kottie.api.ecs

import java.util.UUID

/**
 * Kottie-managed entity that can carry typed components.
 *
 * This is an opt-in data layer. It does not replace the underlying server
 * entity implementation. By default, one component per [ComponentKey] is
 * allowed. Prefer immutable component data classes for predictable snapshots.
 *
 * Thread ownership: the runtime may confine mutations to the server thread
 * when the entity is bound to a live server object. For pure Kottie metadata
 * entities, concurrent access is supported by the default store.
 */
interface Entity {
    val uniqueId: UUID

    fun <T : Component> get(key: ComponentKey<T>): T?
    fun <T : Component> set(key: ComponentKey<T>, component: T)
    fun <T : Component> remove(key: ComponentKey<T>): T?
    fun <T : Component> has(key: ComponentKey<T>): Boolean

    /**
     * Immutable snapshot of current components (key id → component).
     * Safe to share across threads; does not track later mutations.
     */
    fun snapshot(): EntitySnapshot
}

/**
 * Point-in-time copy of an entity's components.
 */
data class EntitySnapshot(
    val uniqueId: UUID,
    val components: Map<String, Component>
) {
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> get(key: ComponentKey<T>): T? =
        components[key.id] as T?
}
