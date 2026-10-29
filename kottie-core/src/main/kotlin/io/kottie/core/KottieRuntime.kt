package io.kottie.core

import io.kottie.api.Kottie
import io.kottie.api.event.EventBus
import io.kottie.api.lifecycle.LifecycleState
import io.kottie.api.lifecycle.PluginLifecycle
import io.kottie.api.scheduler.ServerScheduler
import io.kottie.core.event.DefaultEventBus
import io.kottie.core.scheduler.InMemoryServerScheduler
import io.kottie.coroutines.createPluginScope
import io.kottie.server.ServerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

/**
 * Minimal runtime that wires the core services.
 * In a full implementation this would load plugins, resolve dependencies,
 * and bind a real ServerAdapter.
 */
class KottieRuntime(
    private val adapter: ServerAdapter? = null
) : Kottie, PluginLifecycle {

    private val log = LoggerFactory.getLogger(KottieRuntime::class.java)
    private val stateRef = AtomicReference(LifecycleState.DISCOVERED)

    override val state: LifecycleState get() = stateRef.get()

    private val ownedScope = createPluginScope("kottie-runtime")
    override val scope: CoroutineScope get() = ownedScope

    private val internalScheduler = InMemoryServerScheduler()
    override val scheduler: ServerScheduler
        get() = adapter?.scheduler ?: internalScheduler

    override val eventBus: EventBus = DefaultEventBus(ownedScope)

    override val lifecycle: PluginLifecycle get() = this

    override suspend fun onEnable() {
        if (!stateRef.compareAndSet(LifecycleState.DISCOVERED, LifecycleState.STARTING) &&
            !stateRef.compareAndSet(LifecycleState.RESOLVING, LifecycleState.STARTING)
        ) {
            error("Cannot enable from state $state")
        }
        try {
            adapter?.initialize()
            stateRef.set(LifecycleState.RUNNING)
            Kottie.setInstance(this)
            log.info("Kottie runtime started (adapter={})", adapter?.serverName ?: "in-memory")
        } catch (t: Throwable) {
            stateRef.set(LifecycleState.FAILED)
            throw t
        }
    }

    override suspend fun onDisable() {
        if (!stateRef.compareAndSet(LifecycleState.RUNNING, LifecycleState.STOPPING)) {
            log.warn("Disable called from unexpected state {}", state)
        }
        try {
            // Cancel all background work
            ownedScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            adapter?.shutdown()
            (scheduler as? InMemoryServerScheduler)?.shutdown()
            Kottie.clearInstance()
            stateRef.set(LifecycleState.STOPPED)
            log.info("Kottie runtime stopped")
        } catch (t: Throwable) {
            stateRef.set(LifecycleState.FAILED)
            throw t
        }
    }

    companion object {
        /**
         * Convenience bootstrap for tests and standalone use.
         */
        fun start(adapter: ServerAdapter? = null): KottieRuntime {
            val runtime = KottieRuntime(adapter)
            runBlocking { runtime.onEnable() }
            return runtime
        }
    }
}
