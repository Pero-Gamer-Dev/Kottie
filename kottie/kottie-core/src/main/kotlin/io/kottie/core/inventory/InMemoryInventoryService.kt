package io.kottie.core.inventory

import io.kottie.api.inventory.InventoryService
import io.kottie.api.inventory.InventorySnapshot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryInventoryService : InventoryService {
    private val store = ConcurrentHashMap<UUID, InventorySnapshot>()

    override fun snapshotPlayerInventory(playerId: UUID): InventorySnapshot? =
        store[playerId]

    override fun applyPlayerInventory(playerId: UUID, snapshot: InventorySnapshot): Boolean {
        store[playerId] = snapshot
        return true
    }

    fun clear() = store.clear()
}
