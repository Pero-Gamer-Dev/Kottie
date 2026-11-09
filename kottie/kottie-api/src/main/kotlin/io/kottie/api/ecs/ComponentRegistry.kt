package io.kottie.api.ecs

/**
 * Registry of unique [ComponentKey]s.
 *
 * Keys must be registered before use in production code. Conflicting
 * registrations (same id, different type or duplicate id) are rejected.
 */
interface ComponentRegistry {
    /**
     * Register a key. Throws [IllegalStateException] if the id is already
     * registered to a different key instance or type.
     */
    fun <T : Component> register(key: ComponentKey<T>): ComponentKey<T>

    /**
     * Returns true if a key with this id is already registered.
     */
    fun isRegistered(id: String): Boolean

    /**
     * Lookup by id, or null if unknown.
     */
    fun find(id: String): ComponentKey<*>?

    /**
     * All registered keys (snapshot).
     */
    fun allKeys(): Set<ComponentKey<*>>
}
