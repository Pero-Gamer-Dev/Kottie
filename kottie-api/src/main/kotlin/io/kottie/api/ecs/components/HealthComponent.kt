package io.kottie.api.ecs.components

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey

data class HealthComponent(
    val maxHealth: Double,
    val currentHealth: Double
) : Component {
    init {
        require(maxHealth > 0.0) { "maxHealth must be positive" }
        require(currentHealth in 0.0..maxHealth) { "currentHealth out of range" }
    }

    fun withDamage(amount: Double): HealthComponent {
        val next = (currentHealth - amount).coerceIn(0.0, maxHealth)
        return copy(currentHealth = next)
    }

    fun withHeal(amount: Double): HealthComponent {
        val next = (currentHealth + amount).coerceIn(0.0, maxHealth)
        return copy(currentHealth = next)
    }

    val isDead: Boolean get() = currentHealth <= 0.0

    companion object {
        @JvmField
        val KEY: ComponentKey<HealthComponent> = ComponentKey("kottie:health")
    }
}
