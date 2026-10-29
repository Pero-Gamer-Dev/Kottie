package io.kottie.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal typed configuration holder with atomic snapshot publication.
 * Full hot-reload + validation + migration will be added in later phases.
 */
class ConfigService {

    private val log = LoggerFactory.getLogger(ConfigService::class.java)
    private val holders = ConcurrentHashMap<Class<*>, ConfigHolder<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> bind(type: Class<T>, initial: T): StateFlow<T> {
        val holder = holders.computeIfAbsent(type) {
            ConfigHolder(initial)
        } as ConfigHolder<T>
        return holder.flow
    }

    fun <T : Any> current(type: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return (holders[type] as? ConfigHolder<T>)?.flow?.value
    }

    /**
     * Atomically replace the configuration if the new value is accepted.
     * In a full implementation this would run validation first.
     */
    fun <T : Any> publish(type: Class<T>, value: T) {
        @Suppress("UNCHECKED_CAST")
        val holder = holders[type] as? ConfigHolder<T>
            ?: error("No holder registered for ${type.name}. Call bind() first.")
        holder.flow.value = value
        log.info("Published new config snapshot for {}", type.simpleName)
    }

    private class ConfigHolder<T : Any>(initial: T) {
        val flow = MutableStateFlow(initial)
    }
}
