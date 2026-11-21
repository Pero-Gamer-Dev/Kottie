package io.kottie.core.command

import io.kottie.api.command.CommandContext
import io.kottie.api.command.CommandExecutor
import io.kottie.api.command.CommandManager
import io.kottie.api.command.CommandMeta
import io.kottie.api.command.CommandResult
import io.kottie.api.command.RegisteredCommand
import io.kottie.api.command.TabCompleter
import io.kottie.api.permission.PermissionService
import java.util.concurrent.ConcurrentHashMap

class DefaultCommandManager(
    private val permissions: PermissionService? = null
) : CommandManager {

    private data class Reg(
        override val meta: CommandMeta,
        override val executor: CommandExecutor,
        override val tabCompleter: TabCompleter?
    ) : RegisteredCommand

    private val byName = ConcurrentHashMap<String, Reg>()

    override fun register(
        meta: CommandMeta,
        executor: CommandExecutor,
        tabCompleter: TabCompleter?
    ): RegisteredCommand {
        require(meta.name.isNotBlank()) { "command name blank" }
        val reg = Reg(meta, executor, tabCompleter)
        byName[meta.name.lowercase()] = reg
        meta.aliases.forEach { byName[it.lowercase()] = reg }
        return reg
    }

    override fun unregister(name: String): Boolean {
        val reg = byName.remove(name.lowercase()) ?: return false
        byName.entries.removeIf { it.value === reg }
        return true
    }

    override fun get(name: String): RegisteredCommand? = byName[name.lowercase()]

    override fun commands(): Collection<RegisteredCommand> =
        byName.values.distinctBy { it.meta.name }

    override suspend fun dispatch(ctx: CommandContext): CommandResult {
        val reg = byName[ctx.label.lowercase()] ?: return CommandResult.USAGE
        val perm = reg.meta.permission
        if (perm != null && permissions != null) {
            val allowed = when {
                ctx.senderId != null -> permissions.has(ctx.senderId, perm)
                else -> permissions.has(ctx.senderName, perm)
            }
            if (!allowed) return CommandResult.NO_PERMISSION
        }
        return try {
            reg.executor.execute(ctx)
        } catch (_: Throwable) {
            CommandResult.FAILURE
        }
    }
}
