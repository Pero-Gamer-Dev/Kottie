package io.kottie.testkit

import io.kottie.api.ecs.EntityQuery
import io.kottie.api.ecs.components.CooldownComponent
import io.kottie.api.ecs.components.HealthComponent
import io.kottie.api.ecs.components.ManaComponent
import io.kottie.api.ecs.components.ShieldComponent
import io.kottie.api.ecs.components.StatsComponent
import io.kottie.ecs.DefaultEntityStore
import io.kottie.ecs.SystemRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EcsQueryTest {

    @Test
    fun `query allOf health and mana`() {
        val store = DefaultEntityStore()
        val a = store.create()
        a.set(HealthComponent.KEY, HealthComponent(20.0, 20.0))
        a.set(ManaComponent.KEY, ManaComponent(100, 50))

        val b = store.create()
        b.set(HealthComponent.KEY, HealthComponent(10.0, 10.0))

        val hits = store.query(EntityQuery.allOf(HealthComponent.KEY, ManaComponent.KEY))
        assertEquals(1, hits.size)
        assertEquals(a.uniqueId, hits.first().uniqueId)
    }

    @Test
    fun `query excludes keys`() {
        val store = DefaultEntityStore()
        val a = store.create()
        a.set(HealthComponent.KEY, HealthComponent(20.0, 20.0))
        a.set(ShieldComponent.KEY, ShieldComponent(5.0, 5.0))

        val b = store.create()
        b.set(HealthComponent.KEY, HealthComponent(10.0, 10.0))

        val hits = store.query(
            EntityQuery(
                required = setOf(HealthComponent.KEY),
                excluded = setOf(ShieldComponent.KEY)
            )
        )
        assertEquals(1, hits.size)
        assertEquals(b.uniqueId, hits.first().uniqueId)
    }

    @Test
    fun `index updates on remove component`() {
        val store = DefaultEntityStore()
        val e = store.create()
        e.set(HealthComponent.KEY, HealthComponent(5.0, 5.0))
        assertEquals(1, store.withAll(HealthComponent.KEY).size)
        e.remove(HealthComponent.KEY)
        assertEquals(0, store.withAll(HealthComponent.KEY).size)
    }

    @Test
    fun `cooldown tick and stats value`() {
        var cd = CooldownComponent().trigger("dash", 1000)
        assertFalse(cd.isReady("dash"))
        cd = cd.tick(400)
        assertEquals(600, cd.remainingMs["dash"])
        cd = cd.tick(600)
        assertTrue(cd.isReady("dash"))

        val stats = StatsComponent(base = mapOf("strength" to 10.0))
            .withFlatModifier("strength", 2.0)
        assertEquals(12.0, stats.value("strength"), 1e-9)
    }

    @Test
    fun `health damage and shield absorb`() {
        val h = HealthComponent(20.0, 20.0).withDamage(7.0)
        assertEquals(13.0, h.currentHealth, 1e-9)

        val (shield, remaining) = ShieldComponent(10.0, 4.0).absorb(6.0)
        assertEquals(0.0, shield.currentShield, 1e-9)
        assertEquals(2.0, remaining, 1e-9)
    }

    @Test
    fun `system runner ticks matching entities`() {
        val store = DefaultEntityStore()
        val e = store.create()
        e.set(CooldownComponent.KEY, CooldownComponent().trigger("x", 500))

        val runner = SystemRunner(store)
        runner.register(EntityQuery.allOf(CooldownComponent.KEY)) { entities, delta ->
            entities.forEach { ent ->
                val c = ent.get(CooldownComponent.KEY) ?: return@forEach
                ent.set(CooldownComponent.KEY, c.tick(delta))
            }
        }
        runner.tick(500)
        assertTrue(e.get(CooldownComponent.KEY)!!.isReady("x"))
    }
}
