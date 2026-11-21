package io.kottie.core.permission

import io.kottie.api.permission.PermissionService
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory permissions for tests and simple hosts.
 * Production hosts should wrap LuckPerms / Bukkit Permissible.
 */
class DefaultPermissionService : PermissionService {
    private val byPlayer = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val byName = ConcurrentHashMap<String, MutableSet<String>>()
    private val op = ConcurrentHashMap.newKeySet<UUID>()

    fun setOp(playerId: UUID, value: Boolean = true) {
        if (value) op.add(playerId) else op.remove(playerId)
    }

    override fun has(playerId: UUID, permission: String): Boolean {
        if (playerId in op) return true
        val set = byPlayer[playerId] ?: return false
        return permission in set || "*" in set
    }

    override fun has(senderName: String, permission: String): Boolean {
        val set = byName[senderName.lowercase()] ?: return false
        return permission in set || "*" in set
    }

    override fun grant(playerId: UUID, permission: String) {
        byPlayer.computeIfAbsent(playerId) { ConcurrentHashMap.newKeySet() }.add(permission)
    }

    override fun revoke(playerId: UUID, permission: String) {
        byPlayer[playerId]?.remove(permission)
    }
}
