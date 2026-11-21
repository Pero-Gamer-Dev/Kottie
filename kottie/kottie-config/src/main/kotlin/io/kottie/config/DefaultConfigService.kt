package io.kottie.config

import io.kottie.api.config.ConfigError
import io.kottie.api.config.ConfigLoader
import io.kottie.api.config.ConfigService
import io.kottie.api.config.ConfigValidator
import io.kottie.api.config.ReloadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Typed configuration service with validation and atomic snapshot publication.
 *
 * Invalid reloads retain the last valid value. Valid reloads publish exactly
 * one new immutable snapshot to observers.
 */
class DefaultConfigService : ConfigService {

    private val log = LoggerFactory.getLogger(DefaultConfigService::class.java)
    private val holders = ConcurrentHashMap<Class<*>, Holder<*>>()

    private class Holder<T : Any>(
        initial: T,
        val validator: ConfigValidator<T>
    ) {
        private val mutable = MutableStateFlow(initial)
        val flow: StateFlow<T> = mutable.asStateFlow()
        val current: T get() = mutable.value

        fun tryPublish(candidate: T): ReloadResult<T> {
            val errors = validator.validate(candidate)
            if (errors.isNotEmpty()) {
                return ReloadResult.Failure(errors, retained = mutable.value)
            }
            mutable.value = candidate
            return ReloadResult.Success(candidate)
        }
    }

    override fun <T : Any> bind(
        type: Class<T>,
        initial: T,
        validator: ConfigValidator<T>
    ): StateFlow<T> {
        val errors = validator.validate(initial)
        require(errors.isEmpty()) {
            "Initial config for ${type.simpleName} is invalid: $errors"
        }
        @Suppress("UNCHECKED_CAST")
        val holder = holders.computeIfAbsent(type) {
            Holder(initial, validator)
        } as Holder<T>
        return holder.flow
    }

    override fun <T : Any> current(type: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return (holders[type] as? Holder<T>)?.current
    }

    override fun <T : Any> observe(type: Class<T>): StateFlow<T>? {
        @Suppress("UNCHECKED_CAST")
        return (holders[type] as? Holder<T>)?.flow
    }

    override fun <T : Any> reload(
        type: Class<T>,
        path: Path,
        loader: ConfigLoader<T>
    ): ReloadResult<T> {
        @Suppress("UNCHECKED_CAST")
        val holder = holders[type] as? Holder<T>
            ?: return ReloadResult.Failure(
                listOf(ConfigError("", "No holder registered for ${type.name}. Call bind() first.")),
                retained = null
            )

        val candidate = try {
            loader.load(path)
        } catch (t: Throwable) {
            log.warn("Failed to load config from {}: {}", path, t.message)
            return ReloadResult.Failure(
                listOf(ConfigError(path.toString(), t.message ?: "load failed")),
                retained = holder.current
            )
        }

        val result = holder.tryPublish(candidate)
        when (result) {
            is ReloadResult.Success ->
                log.info("Published new config snapshot for {} from {}", type.simpleName, path)
            is ReloadResult.Failure ->
                log.warn(
                    "Rejected config for {} from {}; retaining last valid snapshot ({} error(s))",
                    type.simpleName, path, result.errors.size
                )
        }
        return result
    }

    override fun <T : Any> publish(type: Class<T>, value: T): ReloadResult<T> {
        @Suppress("UNCHECKED_CAST")
        val holder = holders[type] as? Holder<T>
            ?: return ReloadResult.Failure(
                listOf(ConfigError("", "No holder registered for ${type.name}. Call bind() first.")),
                retained = null
            )
        val result = holder.tryPublish(value)
        if (result is ReloadResult.Success) {
            log.info("Published new config snapshot for {}", type.simpleName)
        }
        return result
    }
}
