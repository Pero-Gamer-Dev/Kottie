package io.kottie.api.ecs

import java.util.UUID

/**
 * Kottie-managed entity that can carry typed components.
 *
 * This is intentionally an opt-in data layer. It does not replace the underlying
 * server entity implementation. Thread confinement and lifecycle rules are
 * enforced by the runtime (see documentation).
 */
interface Entity {
    val uniqueId: UUID

    fun <T : Component> get(key: ComponentKey<T>): T?
    fun <T : Component> set(key: ComponentKey<T>, component: T)
    fun <T : Component> remove(key: ComponentKey<T>): T?
    fun <T : Component> has(key: ComponentKey<T>): Boolean
}
