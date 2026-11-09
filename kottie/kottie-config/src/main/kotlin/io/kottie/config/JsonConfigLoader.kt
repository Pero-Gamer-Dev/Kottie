package io.kottie.config

import io.kottie.api.config.ConfigLoader
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * kotlinx.serialization-based JSON loader.
 * The target type must be annotated with @Serializable.
 */
class JsonConfigLoader<T : Any>(
    private val type: KClass<T>,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true }
) : ConfigLoader<T> {

    @Suppress("UNCHECKED_CAST")
    override fun load(path: Path): T {
        val text = Files.readString(path)
        val serializer = json.serializersModule.serializer(type.java)
        return json.decodeFromString(serializer, text) as T
    }
}

inline fun <reified T : Any> jsonConfigLoader(
    json: Json = Json { ignoreUnknownKeys = true; prettyPrint = true }
): ConfigLoader<T> = JsonConfigLoader(T::class, json)
