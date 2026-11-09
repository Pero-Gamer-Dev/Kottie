package io.kottie.core

import io.kottie.api.Kottie
import io.kottie.api.diagnostics.DiagnosticEvent
import io.kottie.api.diagnostics.DiagnosticSink
import io.kottie.api.ecs.ComponentRegistry
import io.kottie.api.config.ConfigService
import io.kottie.api.ecs.EntityStore
import io.kottie.api.event.EventBus
import io.kottie.api.lifecycle.LifecycleState
import io.kottie.api.lifecycle.PluginLifecycle
import io.kottie.api.scheduler.ServerScheduler
import io.kottie.core.event.DefaultEventBus
import io.kottie.core.scheduler.InMemoryServerScheduler
import io.kottie.coroutines.CancellableScope
import io.kottie.coroutines.createPluginScope
import io.kottie.ecs.DefaultComponentRegistry
import io.kottie.config.DefaultConfigService
import io.kottie.ecs.DefaultEntityStore
import io.kottie.server.ServerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Runtime that owns lifecycle, scopes, scheduler, event bus, and ECS baseline.
 */
class KottieRuntime(
    private val adapter: ServerAdapter? = null,
    private val diagnosticSink: DiagnosticSink? = null
) : Kottie, PluginLifecycle {

    private val log = LoggerFactory.getLogger(KottieRuntime::class.java)
    private val stateRef = AtomicReference(LifecycleState.DISCOVERED)
    private val sinks = CopyOnWriteArrayList<DiagnosticSink>()

    init {
        diagnosticSink?.let { sinks.add(it) }
        sinks.add(DiagnosticSink { event ->
            when (event.category) {
                DiagnosticEvent.Category.ERROR ->
                    log.error("{} {}", event.message, event.attributes, event.throwable)
                DiagnosticEvent.Category.SHUTDOWN, DiagnosticEvent.Category.CANCELLATION ->
                    log.warn("{} {}", event.message, event.attributes)
                else ->
                    log.info("{} {}", event.message, event.attributes)
            }
        })
    }

    override val state: LifecycleState get() = stateRef.get()

    private val rawScope = createPluginScope("kottie-runtime")
    private val cancellableScope = CancellableScope(rawScope)
    override val scope: CoroutineScope get() = cancellableScope

    private val internalScheduler = InMemoryServerScheduler()
    override val scheduler: ServerScheduler
        get() = adapter?.scheduler ?: internalScheduler

    override val eventBus: EventBus = DefaultEventBus(rawScope)

    override val entityStore: EntityStore = DefaultEntityStore()
    override val componentRegistry: ComponentRegistry = DefaultComponentRegistry()
    override val config: ConfigService = DefaultConfigService()

    override val lifecycle: PluginLifecycle get() = this

    fun addDiagnosticSink(sink: DiagnosticSink) {
        sinks.add(sink)
    }

    private fun report(
        category: DiagnosticEvent.Category,
        message: String,
        t: Throwable? = null,
        attrs: Map<String, String> = emptyMap()
    ) {
        val event = DiagnosticEvent(category, message, t, attrs)
        sinks.forEach { sink ->
            try {
                sink.report(event)
            } catch (_: Throwable) {
                // never let a sink crash the runtime
            }
        }
    }

    override suspend fun onEnable() {
        val current = stateRef.get()
        if (current != LifecycleState.DISCOVERED && current != LifecycleState.RESOLVING) {
            error("Cannot enable from state $current")
        }
        if (!stateRef.compareAndSet(current, LifecycleState.STARTING)) {
            error("Concurrent enable transition failed (was $current)")
        }
        report(DiagnosticEvent.Category.LIFECYCLE, "Runtime starting", attrs = mapOf("from" to current.name))

        try {
            adapter?.initialize()
            stateRef.set(LifecycleState.RUNNING)
            Kottie.setInstance(this)
            report(
                DiagnosticEvent.Category.LIFECYCLE,
                "Runtime started",
                attrs = mapOf("adapter" to (adapter?.serverName ?: "in-memory"))
            )
        } catch (t: Throwable) {
            stateRef.set(LifecycleState.FAILED)
            report(DiagnosticEvent.Category.ERROR, "Runtime failed to start", t)
            throw t
        }
    }

    override suspend fun onDisable(timeout: Duration) {
        val current = stateRef.get()
        if (current == LifecycleState.STOPPED || current == LifecycleState.STOPPING) {
            report(DiagnosticEvent.Category.LIFECYCLE, "Disable ignored; already $current")
            return
        }
        if (!stateRef.compareAndSet(current, LifecycleState.STOPPING) &&
            current != LifecycleState.RUNNING &&
            current != LifecycleState.FAILED
        ) {
            report(DiagnosticEvent.Category.LIFECYCLE, "Disable from unexpected state $current")
        } else {
            stateRef.set(LifecycleState.STOPPING)
        }

        report(
            DiagnosticEvent.Category.SHUTDOWN,
            "Runtime shutting down",
            attrs = mapOf("timeoutMs" to timeout.toMillis().toString())
        )

        try {
            val unfinished = cancellableScope.cancelAndJoinWithTimeout(timeout, "runtime-disable")
            if (unfinished.isNotEmpty()) {
                report(
                    DiagnosticEvent.Category.CANCELLATION,
                    "Shutdown timeout; ${unfinished.size} job(s) still active",
                    attrs = mapOf(
                        "unfinished" to unfinished.size.toString(),
                        "timeoutMs" to timeout.toMillis().toString()
                    )
                )
            } else {
                report(DiagnosticEvent.Category.CANCELLATION, "All runtime jobs cancelled cleanly")
            }

            adapter?.shutdown()
            (scheduler as? InMemoryServerScheduler)?.shutdown()
            Kottie.clearInstance()
            stateRef.set(LifecycleState.STOPPED)
            report(DiagnosticEvent.Category.LIFECYCLE, "Runtime stopped")
        } catch (t: Throwable) {
            stateRef.set(LifecycleState.FAILED)
            report(DiagnosticEvent.Category.ERROR, "Runtime failed during shutdown", t)
            throw t
        }
    }

    companion object {
        fun start(
            adapter: ServerAdapter? = null,
            diagnosticSink: DiagnosticSink? = null
        ): KottieRuntime {
            val runtime = KottieRuntime(adapter, diagnosticSink)
            runBlocking { runtime.onEnable() }
            return runtime
        }
    }
}
