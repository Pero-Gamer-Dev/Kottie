package io.kottie.core.service

import io.kottie.api.service.ServiceRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class DefaultServiceRegistry : ServiceRegistry {

    private data class Entry(val instance: Any, val priority: Int)

    private val map = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<Entry>>()

    override fun <T : Any> register(type: Class<T>, instance: T, priority: Int) {
        val list = map.computeIfAbsent(type) { CopyOnWriteArrayList() }
        list.add(Entry(instance, priority))
        list.sortByDescending { it.priority }
    }

    override fun <T : Any> unregister(type: Class<T>, instance: T) {
        map[type]?.removeIf { it.instance === instance }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(type: Class<T>): T? =
        map[type]?.firstOrNull()?.instance as T?

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getAll(type: Class<T>): List<T> =
        map[type]?.map { it.instance as T }.orEmpty()
}
