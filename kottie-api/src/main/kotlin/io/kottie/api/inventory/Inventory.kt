package io.kottie.api.inventory

/**
 * Platform-neutral item identity (material name or namespaced key).
 * Not a Bukkit ItemStack — hosts map to/from native items.
 */
data class ItemType(
    val id: String
) {
    init {
        require(id.isNotBlank())
    }
}

/**
 * Immutable item stack snapshot.
 */
data class ItemStackSnapshot(
    val type: ItemType,
    val amount: Int = 1,
    val displayName: String? = null,
    /** Arbitrary string meta for plugins (serialized NBT left to host). */
    val meta: Map<String, String> = emptyMap()
) {
    init {
        require(amount >= 0) { "amount must be non-negative" }
    }

    val isEmpty: Boolean get() = amount == 0 || type.id.equals("air", ignoreCase = true)
}

/**
 * Snapshot of an inventory view (player, chest, etc.).
 */
data class InventorySnapshot(
    val title: String = "",
    val size: Int,
    val contents: List<ItemStackSnapshot?>
) {
    init {
        require(size >= 0)
        require(contents.size == size) { "contents size must equal inventory size" }
    }

    fun get(slot: Int): ItemStackSnapshot? = contents.getOrNull(slot)

    fun isEmpty(): Boolean = contents.all { it == null || it.isEmpty }
}

interface InventoryService {
    /**
     * Snapshot a player's inventory. Returns null if the player is not online
     * or the host cannot provide data.
     */
    fun snapshotPlayerInventory(playerId: java.util.UUID): InventorySnapshot?

    /**
     * Apply a full inventory snapshot to a player (host-defined semantics).
     * Default in-memory service stores snapshots only.
     */
    fun applyPlayerInventory(playerId: java.util.UUID, snapshot: InventorySnapshot): Boolean
}
