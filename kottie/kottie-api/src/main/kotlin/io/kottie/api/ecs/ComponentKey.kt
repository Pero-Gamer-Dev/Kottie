package io.kottie.api.ecs

/**
 * Typed key for a component. Keys must be unique by [id].
 *
 * Implemented as a data class (not an inline value class) so keys can be used
 * in vararg query APIs and exposed to Java via `@JvmField` constants.
 */
data class ComponentKey<out T : Component>(val id: String) {
    init {
        require(id.isNotBlank()) { "ComponentKey id must not be blank" }
    }
}
