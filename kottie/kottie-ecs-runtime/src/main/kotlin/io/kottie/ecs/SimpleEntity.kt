package io.kottie.ecs

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey
import io.kottie.api.ecs.Entity
import io.kottie.api.ecs.EntityListener
import io.kottie.api.ecs.EntitySnapshot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Concurrent entity implementation for Kottie-managed metadata.
 * Not a replacement for the server's native entity system.
 *
 * One component per key. Listeners are notified on set/remove when provided
 * by the owning [DefaultEntityStore].
 */
class SimpleEntity(
    override val uniqueId: UUID = UUID.randomUUID(),
    private val listeners: () -> List<EntityListener> = { emptyList() }
) : Entity {

    private val components = ConcurrentHashMap<ComponentKey<*>, Component>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Component> get(key: ComponentKey<T>): T? =
        components[key] as T?

    override fun <T : Component> set(key: ComponentKey<T>, component: T) {
        components[key] = component
        listeners().forEach { listener ->
            try {
                listener.onComponentSet(this, key, component)
            } catch (_: Throwable) {
                // isolation
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Component> remove(key: ComponentKey<T>): T? {
        val previous = components.remove(key) as T?
        listeners().forEach { listener ->
            try {
                listener.onComponentRemoved(this, key, previous)
            } catch (_: Throwable) {
                // isolation
            }
        }
        return previous
    }

    override fun <T : Component> has(key: ComponentKey<T>): Boolean =
        components.containsKey(key)

    override fun snapshot(): EntitySnapshot {
        val map = components.entries.associate { (k, v) -> k.id to v }
        return EntitySnapshot(uniqueId, map)
    }

    internal fun clearComponents() {
        components.clear()
    }
}
