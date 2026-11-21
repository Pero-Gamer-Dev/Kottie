package io.kottie.testkit

import io.kottie.api.inventory.InventorySnapshot
import io.kottie.api.inventory.ItemStackSnapshot
import io.kottie.api.inventory.ItemType
import io.kottie.api.player.GameMode
import io.kottie.api.world.LocationSnapshot
import io.kottie.core.KottieRuntime
import io.kottie.core.player.MutablePlayerView
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

class PlayerWorldInventoryTest {

    @Test
    fun `player service track teleport and lookup`() {
        val runtime = KottieRuntime.start()
        try {
            val id = UUID.randomUUID()
            runtime.players.update(
                MutablePlayerView(
                    uniqueId = id,
                    name = "Steve",
                    location = LocationSnapshot("world", 10.0, 65.0, -3.0)
                )
            )
            assertEquals(1, runtime.players.count())
            assertEquals("Steve", runtime.players.getByName("steve")?.name)
            assertTrue(
                runtime.players.teleport(id, LocationSnapshot("world", 0.0, 70.0, 0.0))
            )
            assertEquals(70.0, runtime.players.get(id)!!.location.y, 1e-9)
            runtime.players.remove(id)
            assertEquals(0, runtime.players.count())
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }

    @Test
    fun `world service has default world`() {
        val runtime = KottieRuntime.start()
        try {
            val w = runtime.worlds.defaultWorld()
            assertNotNull(w)
            assertEquals("world", w!!.name)
            assertEquals(w.uid, runtime.worlds.getByUid(w.uid)?.uid)
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }

    @Test
    fun `inventory snapshot apply and read`() {
        val runtime = KottieRuntime.start()
        try {
            val id = UUID.randomUUID()
            val snap = InventorySnapshot(
                title = "player",
                size = 3,
                contents = listOf(
                    ItemStackSnapshot(ItemType("minecraft:diamond"), 5),
                    null,
                    ItemStackSnapshot(ItemType("minecraft:bread"), 2, displayName = "Lunch")
                )
            )
            assertTrue(runtime.inventories.applyPlayerInventory(id, snap))
            val loaded = runtime.inventories.snapshotPlayerInventory(id)!!
            assertEquals(5, loaded.get(0)?.amount)
            assertEquals("Lunch", loaded.get(2)?.displayName)
            assertTrue(loaded.get(1) == null)
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }

    @Test
    fun `location block coords handle negatives`() {
        val loc = LocationSnapshot("world", -1.5, 64.0, -0.1)
        assertEquals(-2, loc.blockX)
        assertEquals(-1, loc.blockZ)
    }
}
