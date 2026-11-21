package io.kottie.api.plugin

import io.kottie.api.lifecycle.PluginLifecycle
import java.nio.file.Path

/**
 * Declared plugin metadata (similar in spirit to plugin.yml).
 */
data class PluginDescription(
    val id: String,
    val name: String,
    val version: String,
    val mainClass: String? = null,
    val authors: List<String> = emptyList(),
    val depend: List<String> = emptyList(),
    val softDepend: List<String> = emptyList(),
    val loadBefore: List<String> = emptyList(),
    val description: String = ""
)

enum class PluginState {
    LOADED,
    ENABLED,
    DISABLED,
    FAILED
}

/**
 * Loaded plugin handle. [instance] may be a [PluginLifecycle] or host object.
 */
interface LoadedPlugin {
    val description: PluginDescription
    val state: PluginState
    val instance: Any?
    val dataFolder: Path?
}

/**
 * Lightweight plugin loader skeleton — dependency order, enable/disable lifecycle.
 * Does **not** implement a full isolated classloader like Paper's PluginClassLoader.
 */
interface PluginLoader {
    fun register(description: PluginDescription, instance: Any? = null, dataFolder: Path? = null): LoadedPlugin
    fun unload(pluginId: String): Boolean
    fun get(pluginId: String): LoadedPlugin?
    fun plugins(): Collection<LoadedPlugin>

    /**
     * Enable plugins in dependency order. Returns ids that failed.
     */
    suspend fun enableAll(): List<String>

    /**
     * Disable plugins in reverse dependency order.
     */
    suspend fun disableAll()

    suspend fun enable(pluginId: String): Boolean
    suspend fun disable(pluginId: String): Boolean

    /**
     * Topologically sorted plugin ids (depend first).
     * @throws IllegalStateException on cycles
     */
    fun resolveLoadOrder(): List<String>
}
