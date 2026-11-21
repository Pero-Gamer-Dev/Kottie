package io.kottie.api.service

/**
 * Lightweight service registry (similar in spirit to Bukkit ServicesManager).
 */
interface ServiceRegistry {
    fun <T : Any> register(type: Class<T>, instance: T, priority: Int = 0)
    fun <T : Any> unregister(type: Class<T>, instance: T)
    fun <T : Any> get(type: Class<T>): T?
    fun <T : Any> getAll(type: Class<T>): List<T>
}

inline fun <reified T : Any> ServiceRegistry.register(instance: T, priority: Int = 0) =
    register(T::class.java, instance, priority)

inline fun <reified T : Any> ServiceRegistry.get(): T? = get(T::class.java)
