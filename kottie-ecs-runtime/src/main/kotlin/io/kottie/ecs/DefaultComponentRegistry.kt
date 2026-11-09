package io.kottie.ecs

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey
import io.kottie.api.ecs.ComponentRegistry
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe component key registry. Rejects conflicting registrations.
 */
class DefaultComponentRegistry : ComponentRegistry {

    private val byId = ConcurrentHashMap<String, ComponentKey<*>>()

    override fun <T : Component> register(key: ComponentKey<T>): ComponentKey<T> {
        val existing = byId.putIfAbsent(key.id, key)
        if (existing != null && existing != key) {
            throw IllegalStateException(
                "ComponentKey id '${key.id}' is already registered to a different key"
            )
        }
        @Suppress("UNCHECKED_CAST")
        return (existing ?: key) as ComponentKey<T>
    }

    override fun isRegistered(id: String): Boolean = byId.containsKey(id)

    override fun find(id: String): ComponentKey<*>? = byId[id]

    override fun allKeys(): Set<ComponentKey<*>> = byId.values.toSet()
}
