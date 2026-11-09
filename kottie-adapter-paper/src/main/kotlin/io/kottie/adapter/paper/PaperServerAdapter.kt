package io.kottie.adapter.paper

import io.kottie.api.scheduler.ServerScheduler
import io.kottie.server.ServerAdapter
import org.slf4j.LoggerFactory

/**
 * Skeleton Paper/Bukkit adapter for Kottie 1.0.
 *
 * This class documents the integration surface. A full binding requires the
 * Paper API on the classpath and a concrete [ServerScheduler] that dispatches
 * to `Bukkit.getScheduler()` / the server primary thread.
 *
 * Until that binding is supplied, [initialize] fails fast with a clear message
 * so unsupported deployments do not silently misbehave.
 */
class PaperServerAdapter(
    override val supportedVersions: Set<String> = setOf("1.20.4", "1.21"),
    private val schedulerOverride: ServerScheduler? = null,
    private val paperAvailable: Boolean = false
) : ServerAdapter {

    private val log = LoggerFactory.getLogger(PaperServerAdapter::class.java)

    override val serverName: String = "paper"

    override val scheduler: ServerScheduler
        get() = schedulerOverride
            ?: error(
                "PaperServerAdapter has no scheduler. " +
                    "Supply a Paper-backed ServerScheduler or use the in-memory runtime for tests."
            )

    override fun initialize() {
        if (!paperAvailable && schedulerOverride == null) {
            throw UnsupportedServerEnvironmentException(
                "Paper runtime not detected. Supported versions: $supportedVersions. " +
                    "Use KottieRuntime with the in-memory adapter for unit tests, " +
                    "or provide a Paper-backed ServerScheduler."
            )
        }
        log.info("PaperServerAdapter initialized (versions={})", supportedVersions)
    }

    override fun shutdown() {
        log.info("PaperServerAdapter shutdown")
    }
}

class UnsupportedServerEnvironmentException(message: String) : IllegalStateException(message)
