package io.kottie.testkit

import io.kottie.api.config.ConfigError
import io.kottie.api.config.ConfigLoader
import io.kottie.api.config.ConfigValidator
import io.kottie.api.config.ReloadResult
import io.kottie.api.config.bind
import io.kottie.api.config.current
import io.kottie.api.config.publish
import io.kottie.api.config.reload
import io.kottie.config.DefaultConfigService
import io.kottie.core.KottieRuntime
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

data class AppConfig(
    val maxPlayers: Int = 20,
    val motd: String = "Welcome"
)

class ConfigServiceTest {

    private val positivePlayers = ConfigValidator<AppConfig> { cfg ->
        buildList {
            if (cfg.maxPlayers <= 0) add(ConfigError("maxPlayers", "must be positive"))
            if (cfg.motd.isBlank()) add(ConfigError("motd", "must not be blank"))
        }
    }

    @Test
    fun `bind publishes initial snapshot`() {
        val svc = DefaultConfigService()
        val flow = svc.bind(AppConfig::class.java, AppConfig(20, "Hi"), positivePlayers)
        assertEquals(20, flow.value.maxPlayers)
        assertEquals(AppConfig(20, "Hi"), svc.current(AppConfig::class.java))
    }

    @Test
    fun `invalid initial config is rejected`() {
        val svc = DefaultConfigService()
        assertThrows(IllegalArgumentException::class.java) {
            svc.bind(AppConfig::class.java, AppConfig(-1, "x"), positivePlayers)
        }
    }

    @Test
    fun `valid publish updates observers`() {
        val svc = DefaultConfigService()
        val flow = svc.bind(AppConfig::class.java, AppConfig(), positivePlayers)
        val result = svc.publish(AppConfig::class.java, AppConfig(50, "New"))
        assertTrue(result is ReloadResult.Success)
        assertEquals(50, flow.value.maxPlayers)
        assertEquals("New", flow.value.motd)
    }

    @Test
    fun `invalid publish retains last valid snapshot`() {
        val svc = DefaultConfigService()
        val flow = svc.bind(AppConfig::class.java, AppConfig(20, "Ok"), positivePlayers)
        val result = svc.publish(AppConfig::class.java, AppConfig(-5, ""))
        assertTrue(result is ReloadResult.Failure)
        val failure = result as ReloadResult.Failure
        assertEquals(2, failure.errors.size)
        assertEquals(20, flow.value.maxPlayers)
        assertEquals("Ok", flow.value.motd)
        assertEquals(AppConfig(20, "Ok"), failure.retained)
    }

    @Test
    fun `reload from file success and failure`(@TempDir dir: Path) {
        val svc = DefaultConfigService()
        val flow = svc.bind(AppConfig::class.java, AppConfig(10, "Boot"), positivePlayers)

        val good = dir.resolve("good.txt")
        Files.writeString(good, "30|Hello")
        val bad = dir.resolve("bad.txt")
        Files.writeString(bad, "-1|")

        val loader = ConfigLoader<AppConfig> { path ->
            val parts = Files.readString(path).trim().split("|")
            AppConfig(parts[0].toInt(), parts.getOrElse(1) { "" })
        }

        val ok = svc.reload(AppConfig::class.java, good, loader)
        assertTrue(ok is ReloadResult.Success)
        assertEquals(30, flow.value.maxPlayers)

        val fail = svc.reload(AppConfig::class.java, bad, loader)
        assertTrue(fail is ReloadResult.Failure)
        // last valid retained
        assertEquals(30, flow.value.maxPlayers)
        assertEquals("Hello", flow.value.motd)
    }

    @Test
    fun `runtime exposes config service`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val flow = runtime.config.bind(AppConfig(5, "Runtime"), positivePlayers)
            assertEquals(5, flow.value.maxPlayers)
            assertNotNull(runtime.config.current<AppConfig>())
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }
}
