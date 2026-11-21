package io.kottie.core.plugin

import io.kottie.api.lifecycle.PluginLifecycle
import io.kottie.api.plugin.LoadedPlugin
import io.kottie.api.plugin.PluginDescription
import io.kottie.api.plugin.PluginLoader
import io.kottie.api.plugin.PluginState
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class DefaultPluginLoader : PluginLoader {

    private val log = LoggerFactory.getLogger(DefaultPluginLoader::class.java)

    private class Handle(
        override val description: PluginDescription,
        override var state: PluginState = PluginState.LOADED,
        override var instance: Any? = null,
        override val dataFolder: Path? = null
    ) : LoadedPlugin

    private val plugins = ConcurrentHashMap<String, Handle>()

    override fun register(
        description: PluginDescription,
        instance: Any?,
        dataFolder: Path?
    ): LoadedPlugin {
        require(description.id.isNotBlank())
        require(!plugins.containsKey(description.id)) { "plugin already registered: ${description.id}" }
        val h = Handle(description, PluginState.LOADED, instance, dataFolder)
        plugins[description.id] = h
        return h
    }

    override fun unload(pluginId: String): Boolean {
        val h = plugins[pluginId] ?: return false
        if (h.state == PluginState.ENABLED) {
            throw IllegalStateException("disable plugin before unload: $pluginId")
        }
        plugins.remove(pluginId)
        return true
    }

    override fun get(pluginId: String): LoadedPlugin? = plugins[pluginId]

    override fun plugins(): Collection<LoadedPlugin> = plugins.values.toList()

    override fun resolveLoadOrder(): List<String> {
        val ids = plugins.keys.toSet()
        val deps = plugins.mapValues { (_, h) ->
            (h.description.depend + h.description.softDepend.filter { it in ids }).distinct()
        }
        val result = ArrayList<String>()
        val visiting = LinkedHashSet<String>()
        val visited = HashSet<String>()

        fun visit(id: String) {
            if (id in visited) return
            if (id in visiting) throw IllegalStateException("plugin dependency cycle at $id")
            visiting.add(id)
            for (d in deps[id].orEmpty()) {
                if (d !in ids && d in plugins[id]!!.description.depend) {
                    throw IllegalStateException("missing hard dependency $d for $id")
                }
                if (d in ids) visit(d)
            }
            visiting.remove(id)
            visited.add(id)
            result.add(id)
        }

        ids.forEach { visit(it) }
        return result
    }

    override suspend fun enableAll(): List<String> {
        val failed = mutableListOf<String>()
        for (id in resolveLoadOrder()) {
            if (!enable(id)) failed.add(id)
        }
        return failed
    }

    override suspend fun disableAll() {
        for (id in resolveLoadOrder().asReversed()) {
            disable(id)
        }
    }

    override suspend fun enable(pluginId: String): Boolean {
        val h = plugins[pluginId] ?: return false
        if (h.state == PluginState.ENABLED) return true
        return try {
            val lifecycle = h.instance as? PluginLifecycle
            lifecycle?.onEnable()
            h.state = PluginState.ENABLED
            true
        } catch (t: Throwable) {
            log.error("Failed to enable plugin {}", pluginId, t)
            h.state = PluginState.FAILED
            false
        }
    }

    override suspend fun disable(pluginId: String): Boolean {
        val h = plugins[pluginId] ?: return false
        if (h.state != PluginState.ENABLED) return true
        return try {
            val lifecycle = h.instance as? PluginLifecycle
            lifecycle?.onDisable(Duration.ofSeconds(5))
            h.state = PluginState.DISABLED
            true
        } catch (t: Throwable) {
            log.error("Failed to disable plugin {}", pluginId, t)
            h.state = PluginState.FAILED
            false
        }
    }
}
