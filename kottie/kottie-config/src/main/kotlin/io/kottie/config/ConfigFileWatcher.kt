package io.kottie.config

import io.kottie.api.config.ConfigLoader
import io.kottie.api.config.ConfigService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Debounced file watcher that triggers [ConfigService.reload] on changes.
 * Call [close] on shutdown to release the watch service and cancel jobs.
 */
class ConfigFileWatcher(
    private val scope: CoroutineScope,
    private val configService: ConfigService,
    private val debounceMs: Long = 300
) : AutoCloseable {

    private val log = LoggerFactory.getLogger(ConfigFileWatcher::class.java)
    private val watchService = FileSystems.getDefault().newWatchService()
    private val keys = ConcurrentHashMap<WatchKey, Path>()
    private val pending = ConcurrentHashMap<String, Job>()
    private val closed = AtomicBoolean(false)
    private var pollJob: Job? = null

    fun <T : Any> watch(
        type: Class<T>,
        path: Path,
        loader: ConfigLoader<T>
    ) {
        if (closed.get()) error("Watcher is closed")
        val abs = path.toAbsolutePath().normalize()
        val dir = abs.parent ?: error("Config path has no parent: $abs")
        val key = dir.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )
        keys[key] = dir
        log.info("Watching {} for {}", dir, type.simpleName)

        if (pollJob == null) {
            pollJob = scope.launch { pollLoop(type, abs, loader) }
        }
    }

    private suspend fun <T : Any> pollLoop(type: Class<T>, target: Path, loader: ConfigLoader<T>) {
        while (scope.isActive && !closed.get()) {
            val key = try {
                watchService.take()
            } catch (_: InterruptedException) {
                break
            } catch (_: Exception) {
                break
            }
            val dir = keys[key] ?: continue
            for (event in key.pollEvents()) {
                val name = event.context() as? Path ?: continue
                val changed = dir.resolve(name).normalize()
                if (changed == target) {
                    scheduleReload(type, target, loader)
                }
            }
            if (!key.reset()) {
                keys.remove(key)
            }
        }
    }

    private fun <T : Any> scheduleReload(type: Class<T>, path: Path, loader: ConfigLoader<T>) {
        val id = type.name + ":" + path
        pending[id]?.cancel()
        pending[id] = scope.launch {
            delay(debounceMs)
            if (!closed.get()) {
                val result = configService.reload(type, path, loader)
                log.info("Debounced reload of {} → {}", type.simpleName, result::class.simpleName)
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            pending.values.forEach { it.cancel() }
            pending.clear()
            pollJob?.cancel()
            try {
                watchService.close()
            } catch (_: Exception) {
            }
            keys.clear()
        }
    }
}
