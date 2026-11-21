package io.kottie.api.ecs.components

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey

/**
 * Flat + percent modifiers for named stats (e.g. strength, speed).
 * final = (base + flat) * (1 + percent)
 */
data class StatsComponent(
    val base: Map<String, Double> = emptyMap(),
    val flat: Map<String, Double> = emptyMap(),
    val percent: Map<String, Double> = emptyMap()
) : Component {

    fun value(stat: String): Double {
        val b = base[stat] ?: 0.0
        val f = flat[stat] ?: 0.0
        val p = percent[stat] ?: 0.0
        return (b + f) * (1.0 + p)
    }

    fun withBase(stat: String, value: Double): StatsComponent =
        copy(base = base + (stat to value))

    fun withFlatModifier(stat: String, amount: Double): StatsComponent =
        copy(flat = flat + (stat to ((flat[stat] ?: 0.0) + amount)))

    companion object {
        @JvmField
        val KEY: ComponentKey<StatsComponent> = ComponentKey("kottie:stats")
    }
}
