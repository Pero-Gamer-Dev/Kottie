package io.kottie.api.ecs.components

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey

data class ShieldComponent(
    val maxShield: Double,
    val currentShield: Double
) : Component {
    init {
        require(maxShield >= 0.0)
        require(currentShield in 0.0..maxShield)
    }

    fun absorb(damage: Double): Pair<ShieldComponent, Double> {
        val absorbed = minOf(currentShield, damage)
        val remaining = damage - absorbed
        return copy(currentShield = currentShield - absorbed) to remaining
    }

    companion object {
        @JvmField
        val KEY: ComponentKey<ShieldComponent> = ComponentKey("kottie:shield")
    }
}
