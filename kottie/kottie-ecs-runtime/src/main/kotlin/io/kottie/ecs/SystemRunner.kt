package io.kottie.ecs

import io.kottie.api.ecs.EntityQuery
import io.kottie.api.ecs.EntityStore
import io.kottie.api.ecs.EntitySystem

/**
 * Helper to tick one or more [EntitySystem]s against a store.
 */
class SystemRunner(
    private val store: EntityStore
) {
    private data class Registration(
        val query: EntityQuery,
        val system: EntitySystem
    )

    private val systems = java.util.concurrent.CopyOnWriteArrayList<Registration>()

    fun register(query: EntityQuery, system: EntitySystem): AutoCloseable {
        val reg = Registration(query, system)
        systems.add(reg)
        return AutoCloseable { systems.remove(reg) }
    }

    fun tick(deltaMs: Long) {
        for (reg in systems) {
            val matches = store.query(reg.query)
            try {
                reg.system.tick(matches, deltaMs)
            } catch (_: Throwable) {
                // isolation per system
            }
        }
    }
}
