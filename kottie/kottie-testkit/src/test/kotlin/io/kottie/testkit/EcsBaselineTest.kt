package io.kottie.testkit

import io.kottie.api.ecs.Component
import io.kottie.api.ecs.ComponentKey
import io.kottie.api.ecs.Entity
import io.kottie.api.ecs.EntityListener
import io.kottie.api.ecs.components.ManaComponent
import io.kottie.core.KottieRuntime
import io.kottie.ecs.DefaultComponentRegistry
import io.kottie.ecs.DefaultEntityStore
import io.kottie.ecs.SimpleEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking

class EcsBaselineTest {

    data class HealthComponent(val hp: Int) : Component

    @Test
    fun `component key registry rejects conflicts`() {
        val registry = DefaultComponentRegistry()
        val key1 = ComponentKey<ManaComponent>("kottie:mana")
        val key2 = ComponentKey<ManaComponent>("kottie:mana")

        registry.register(key1)
        // same id different instance → conflict
        assertThrows(IllegalStateException::class.java) {
            registry.register(key2)
        }
        assertTrue(registry.isRegistered("kottie:mana"))
        assertEquals(key1, registry.find("kottie:mana"))
    }

    @Test
    fun `entity store create remove and lookup`() {
        val store = DefaultEntityStore()
        val id = UUID.randomUUID()
        val entity = store.create(id)
        assertEquals(id, entity.uniqueId)
        assertSame(entity, store.get(id))
        assertEquals(1, store.all().size)

        assertTrue(store.remove(entity))
        assertNull(store.get(id))
        assertEquals(0, store.all().size)
        assertFalse(store.remove(entity))
    }

    @Test
    fun `duplicate entity id is rejected`() {
        val store = DefaultEntityStore()
        val id = UUID.randomUUID()
        store.create(id)
        assertThrows(IllegalStateException::class.java) {
            store.create(id)
        }
    }

    @Test
    fun `lifecycle listeners fire`() {
        val store = DefaultEntityStore()
        val created = CopyOnWriteArrayList<UUID>()
        val removed = CopyOnWriteArrayList<UUID>()
        val sets = AtomicInteger(0)

        store.addListener(object : EntityListener {
            override fun onEntityCreated(entity: Entity) {
                created.add(entity.uniqueId)
            }
            override fun onEntityRemoved(entity: Entity) {
                removed.add(entity.uniqueId)
            }
            override fun onComponentSet(entity: Entity, key: ComponentKey<*>, component: Component) {
                sets.incrementAndGet()
            }
        })

        val e = store.create()
        e.set(ManaComponent.KEY, ManaComponent(50, 50))
        store.remove(e)

        assertEquals(listOf(e.uniqueId), created.toList())
        assertEquals(listOf(e.uniqueId), removed.toList())
        assertEquals(1, sets.get())
    }

    @Test
    fun `snapshot is immutable copy`() {
        val entity = SimpleEntity()
        entity.set(ManaComponent.KEY, ManaComponent(100, 80))
        val snap = entity.snapshot()

        assertEquals(entity.uniqueId, snap.uniqueId)
        assertEquals(80, snap.get(ManaComponent.KEY)!!.currentMana)

        entity.set(ManaComponent.KEY, ManaComponent(100, 10))
        // snapshot unchanged
        assertEquals(80, snap.get(ManaComponent.KEY)!!.currentMana)
    }

    @Test
    fun `concurrent component writes are safe`() {
        val entity = SimpleEntity()
        val threads = 8
        val iterations = 200
        val pool = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(threads)

        repeat(threads) { t ->
            pool.submit {
                try {
                    repeat(iterations) { i ->
                        entity.set(ManaComponent.KEY, ManaComponent(100, (t + i) % 101))
                        entity.get(ManaComponent.KEY)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
        pool.shutdown()
        assertTrue(entity.has(ManaComponent.KEY))
    }

    @Test
    fun `runtime exposes entity store and registry`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val entity = runtime.entityStore.create()
            runtime.componentRegistry.register(ManaComponent.KEY)
            entity.set(ManaComponent.KEY, ManaComponent(100, 100))
            assertTrue(entity.has(ManaComponent.KEY))
            assertNotNull(entity.snapshot().get(ManaComponent.KEY))
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }
}
