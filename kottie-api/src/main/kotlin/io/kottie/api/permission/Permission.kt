package io.kottie.api.permission

import java.util.UUID

/**
 * Platform-neutral permission checks. Hosts bridge to Bukkit permissions / LuckPerms.
 */
interface PermissionService {
    fun has(playerId: UUID, permission: String): Boolean
    fun has(senderName: String, permission: String): Boolean

    /**
     * Optional: grant in-memory for tests / simple servers.
     */
    fun grant(playerId: UUID, permission: String) {}
    fun revoke(playerId: UUID, permission: String) {}
}
