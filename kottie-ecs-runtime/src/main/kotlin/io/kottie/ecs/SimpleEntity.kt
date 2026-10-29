package io.kottie.ecs

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey
import io.kottie.api.ecs.Entity
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Basic concurrent entity implementation for Kottie-managed metadata.
 * Not a replacement for the server's native entity system.
 */
class SimpleEntity(
    override val uniqueId: UUID = UUID.randomUUID()
) : Entity {

    private val components = ConcurrentHashMap<ComponentKey<*>, Component>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Component> get(key: ComponentKey<T>): T? =
        components[key] as T?

    override fun <T : Component> set(key: ComponentKey<T>, component: T) {
        components[key] = component
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Component> remove(key: ComponentKey<T>): T? =
        components.remove(key) as T?

    override fun <T : Component> has(key: ComponentKey<T>): Boolean =
        components.containsKey(key)
}
