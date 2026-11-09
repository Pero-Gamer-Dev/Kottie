package io.kottie.api.ecs

/**
 * Typed key for a component. Keys must be registered uniquely.
 */
@JvmInline
value class ComponentKey<T : Component>(val id: String) {
    init {
        require(id.isNotBlank()) { "ComponentKey id must not be blank" }
    }
}
