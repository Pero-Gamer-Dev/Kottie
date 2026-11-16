package io.kottie.adapter.velocity

import io.kottie.api.ecs.EntityStore
import io.kottie.api.event.EventBus
import io.kottie.api.scheduler.ServerScheduler
import io.kottie.server.ServerAdapter
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.Executor

/**
 * Velocity (and similar proxy) [ServerAdapter] for Kottie 1.9.
 *
 * No compile-time dependency on Velocity API. Host wires executors and
 * forwards login/disconnect into [VelocityEventBridge].
 *
 * Pair with `kottie-cluster` + `kottie-network` for cross-proxy presence.
 */
class VelocityServerAdapter(
    private val schedulerImpl: ServerScheduler? = null,
    override val supportedVersions: Set<String> = DEFAULT_VERSIONS,
    private val requireLiveProxy: Boolean = false,
    private val liveProxyDetected: Boolean = false
) : ServerAdapter {

    private val log = LoggerFactory.getLogger(VelocityServerAdapter::class.java)

    override val serverName: String = "velocity"

    override val scheduler: ServerScheduler
        get() = schedulerImpl
            ?: error(
                "VelocityServerAdapter has no ServerScheduler. " +
                    "Use VelocityServerScheduler wired to the proxy executor."
            )

    @Volatile
    private var eventBridge: VelocityEventBridge? = null

    fun attachEventBridge(
        eventBus: EventBus,
        entityStore: EntityStore,
        scope: CoroutineScope,
        onLoginNotify: (suspend (UUID, String) -> Unit)? = null,
        onDisconnectNotify: (suspend (UUID, String) -> Unit)? = null
    ): VelocityEventBridge {
        val bridge = VelocityEventBridge(
            eventBus, entityStore, scope, onLoginNotify, onDisconnectNotify
        )
        eventBridge = bridge
        return bridge
    }

    fun eventBridge(): VelocityEventBridge? = eventBridge

    override fun initialize() {
        if (requireLiveProxy && !liveProxyDetected) {
            throw UnsupportedProxyEnvironmentException(
                "Velocity live proxy not detected. Supported: $supportedVersions"
            )
        }
        if (schedulerImpl == null) {
            throw UnsupportedProxyEnvironmentException(
                "No ServerScheduler provided to VelocityServerAdapter. Supported: $supportedVersions"
            )
        }
        log.info(
            "VelocityServerAdapter initialized (versions={}, live={})",
            supportedVersions,
            liveProxyDetected
        )
    }

    override fun shutdown() {
        eventBridge?.clear()
        eventBridge = null
        (schedulerImpl as? VelocityServerScheduler)?.shutdown()
        log.info("VelocityServerAdapter shutdown")
    }

    companion object {
        val DEFAULT_VERSIONS: Set<String> = setOf("3.3", "3.4")

        fun forVelocityHost(
            proxyExecutor: Executor,
            isProxyThread: () -> Boolean,
            supportedVersions: Set<String> = DEFAULT_VERSIONS
        ): VelocityServerAdapter {
            val scheduler = VelocityServerScheduler(proxyExecutor, isProxyThread)
            return VelocityServerAdapter(
                schedulerImpl = scheduler,
                supportedVersions = supportedVersions,
                liveProxyDetected = true
            )
        }
    }
}

class UnsupportedProxyEnvironmentException(message: String) : IllegalStateException(message)
