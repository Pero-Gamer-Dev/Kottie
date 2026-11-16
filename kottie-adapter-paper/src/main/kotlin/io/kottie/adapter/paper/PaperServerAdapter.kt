package io.kottie.adapter.paper

import io.kottie.api.event.EventBus
import io.kottie.api.ecs.EntityStore
import io.kottie.api.scheduler.ServerScheduler
import io.kottie.server.ServerAdapter
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.util.concurrent.Executor

/**
 * Paper-oriented [ServerAdapter] for Kottie 1.1.
 *
 * ## Wiring on a real Paper server
 *
 * ```kotlin
 * val scheduler = PaperServerScheduler(
 *     mainThreadExecutor = Executor { task ->
 *         Bukkit.getScheduler().runTask(plugin, task)
 *     },
 *     isPrimaryThread = { Bukkit.isPrimaryThread() }
 * )
 * val adapter = PaperServerAdapter(
 *     scheduler = scheduler,
 *     supportedVersions = setOf("1.20.4", "1.21", "1.21.1")
 * )
 * val runtime = KottieRuntime.start(adapter)
 * val bridge = adapter.attachEventBridge(runtime.eventBus, runtime.entityStore, runtime.scope)
 * // From Bukkit listeners: bridge.onPlayerJoin(...) / bridge.onPlayerQuit(...)
 * ```
 *
 * Without a scheduler (or when [requireLiveServer] is true and the host
 * signals no Paper), [initialize] fails fast.
 */
class PaperServerAdapter(
    private val schedulerImpl: ServerScheduler? = null,
    override val supportedVersions: Set<String> = DEFAULT_VERSIONS,
    private val requireLiveServer: Boolean = false,
    private val liveServerDetected: Boolean = false
) : ServerAdapter {

    private val log = LoggerFactory.getLogger(PaperServerAdapter::class.java)

    override val serverName: String = "paper"

    override val scheduler: ServerScheduler
        get() = schedulerImpl
            ?: error(
                "PaperServerAdapter has no ServerScheduler. " +
                    "Construct with PaperServerScheduler wired to Bukkit.getScheduler()."
            )

    @Volatile
    private var eventBridge: PaperEventBridge? = null

    /**
     * Attach join/quit bridging. Call after the Kottie runtime is started.
     */
    fun attachEventBridge(
        eventBus: EventBus,
        entityStore: EntityStore,
        scope: CoroutineScope
    ): PaperEventBridge {
        val bridge = PaperEventBridge(eventBus, entityStore, scope)
        eventBridge = bridge
        return bridge
    }

    fun eventBridge(): PaperEventBridge? = eventBridge

    override fun initialize() {
        if (requireLiveServer && !liveServerDetected) {
            throw UnsupportedServerEnvironmentException(
                "Paper live server not detected. Supported versions: $supportedVersions"
            )
        }
        if (schedulerImpl == null) {
            throw UnsupportedServerEnvironmentException(
                "No ServerScheduler provided to PaperServerAdapter. " +
                    "Supported versions: $supportedVersions. " +
                    "For unit tests, pass an in-memory scheduler or PaperServerScheduler with a test executor."
            )
        }
        log.info(
            "PaperServerAdapter initialized (versions={}, live={})",
            supportedVersions,
            liveServerDetected
        )
    }

    override fun shutdown() {
        eventBridge?.clear()
        eventBridge = null
        (schedulerImpl as? PaperServerScheduler)?.shutdown()
        log.info("PaperServerAdapter shutdown")
    }

    companion object {
        val DEFAULT_VERSIONS: Set<String> = setOf("1.20.4", "1.21", "1.21.1")

        /**
         * Factory for production Paper hosts.
         */
        fun forPaperHost(
            mainThreadExecutor: Executor,
            isPrimaryThread: () -> Boolean,
            supportedVersions: Set<String> = DEFAULT_VERSIONS
        ): PaperServerAdapter {
            val scheduler = PaperServerScheduler(mainThreadExecutor, isPrimaryThread)
            return PaperServerAdapter(
                schedulerImpl = scheduler,
                supportedVersions = supportedVersions,
                requireLiveServer = false,
                liveServerDetected = true
            )
        }
    }
}

class UnsupportedServerEnvironmentException(message: String) : IllegalStateException(message)
