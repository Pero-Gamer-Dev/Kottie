package io.kottie.api.ecs.components

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey

/**
 * Named cooldowns in milliseconds remaining.
 */
data class CooldownComponent(
    val remainingMs: Map<String, Long> = emptyMap()
) : Component {

    fun isReady(name: String): Boolean =
        (remainingMs[name] ?: 0L) <= 0L

    fun trigger(name: String, durationMs: Long): CooldownComponent {
        require(durationMs >= 0)
        return copy(remainingMs = remainingMs + (name to durationMs))
    }

    fun tick(deltaMs: Long): CooldownComponent {
        if (remainingMs.isEmpty() || deltaMs <= 0) return this
        val next = remainingMs.mapValues { (_, v) -> (v - deltaMs).coerceAtLeast(0L) }
            .filterValues { it > 0L }
        return copy(remainingMs = next)
    }

    companion object {
        @JvmField
        val KEY: ComponentKey<CooldownComponent> = ComponentKey("kottie:cooldown")
    }
}
