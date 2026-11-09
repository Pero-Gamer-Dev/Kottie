package io.kottie.api.config

import kotlinx.coroutines.flow.StateFlow
import java.nio.file.Path

/**
 * Result of a configuration reload attempt.
 */
sealed class ReloadResult<out T> {
    data class Success<T>(val value: T) : ReloadResult<T>()
    data class Failure(
        val errors: List<ConfigError>,
        val retained: Any?
    ) : ReloadResult<Nothing>()
}

/**
 * Structured validation / parse error.
 */
data class ConfigError(
    val path: String,
    val message: String
)

/**
 * Validates a candidate configuration object.
 * Return an empty list when valid.
 */
fun interface ConfigValidator<T> {
    fun validate(value: T): List<ConfigError>
}

/**
 * Loads a configuration object from a path (e.g. JSON file).
 */
fun interface ConfigLoader<T> {
    fun load(path: Path): T
}

/**
 * Typed configuration service.
 *
 * Contract:
 * - [bind] registers a type with an initial validated snapshot and returns a read-only [StateFlow].
 * - [reload] parses → validates → on success atomically publishes one new snapshot;
 *   on failure keeps the last valid snapshot and returns errors.
 * - Observers always see a consistent, previously validated value.
 */
interface ConfigService {
    fun <T : Any> bind(
        type: Class<T>,
        initial: T,
        validator: ConfigValidator<T> = ConfigValidator { emptyList() }
    ): StateFlow<T>

    fun <T : Any> current(type: Class<T>): T?

    fun <T : Any> observe(type: Class<T>): StateFlow<T>?

    /**
     * Reload from [path] using [loader], validate, and publish atomically if valid.
     */
    fun <T : Any> reload(
        type: Class<T>,
        path: Path,
        loader: ConfigLoader<T>
    ): ReloadResult<T>

    /**
     * Publish a pre-built value after validation (no file I/O).
     */
    fun <T : Any> publish(type: Class<T>, value: T): ReloadResult<T>
}

/** Kotlin reified helpers */
inline fun <reified T : Any> ConfigService.bind(
    initial: T,
    validator: ConfigValidator<T> = ConfigValidator { emptyList() }
): StateFlow<T> = bind(T::class.java, initial, validator)

inline fun <reified T : Any> ConfigService.current(): T? = current(T::class.java)

inline fun <reified T : Any> ConfigService.observe(): StateFlow<T>? = observe(T::class.java)

inline fun <reified T : Any> ConfigService.reload(
    path: Path,
    loader: ConfigLoader<T>
): ReloadResult<T> = reload(T::class.java, path, loader)

inline fun <reified T : Any> ConfigService.publish(value: T): ReloadResult<T> =
    publish(T::class.java, value)
