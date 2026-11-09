package io.kottie.api.ecs.components

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey

/**
 * Example custom component matching the README sample.
 */
data class ManaComponent(
    val maxMana: Int,
    val currentMana: Int
) : Component {
    init {
        require(maxMana >= 0) { "maxMana must be non-negative" }
        require(currentMana in 0..maxMana) { "currentMana must be between 0 and maxMana" }
    }

    companion object {
        val KEY = ComponentKey<ManaComponent>("kottie:mana")
    }
}
