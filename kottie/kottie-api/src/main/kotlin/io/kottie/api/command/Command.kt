package io.kottie.api.command

import java.util.UUID

/**
 * Platform-neutral command API. Hosts map this to Brigadier / Bukkit commands.
 */
data class CommandContext(
    val senderId: UUID?,
    val senderName: String,
    val isPlayer: Boolean,
    val label: String,
    val args: List<String>,
    val rawInput: String
) {
    fun arg(index: Int): String? = args.getOrNull(index)
}

enum class CommandResult {
    SUCCESS,
    FAILURE,
    NO_PERMISSION,
    USAGE
}

fun interface CommandExecutor {
    suspend fun execute(ctx: CommandContext): CommandResult
}

fun interface TabCompleter {
    fun complete(ctx: CommandContext): List<String>
}

data class CommandMeta(
    val name: String,
    val description: String = "",
    val usage: String = "",
    val permission: String? = null,
    val aliases: List<String> = emptyList()
)

interface RegisteredCommand {
    val meta: CommandMeta
    val executor: CommandExecutor
    val tabCompleter: TabCompleter?
}

interface CommandManager {
    fun register(meta: CommandMeta, executor: CommandExecutor, tabCompleter: TabCompleter? = null): RegisteredCommand
    fun unregister(name: String): Boolean
    fun get(name: String): RegisteredCommand?
    fun commands(): Collection<RegisteredCommand>
    /**
     * Dispatch a command line (without leading slash). Used by hosts and tests.
     */
    suspend fun dispatch(ctx: CommandContext): CommandResult
}
