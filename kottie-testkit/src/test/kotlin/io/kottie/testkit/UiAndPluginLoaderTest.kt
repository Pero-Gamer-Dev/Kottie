package io.kottie.testkit

import io.kottie.api.bossbar.BossBarColor
import io.kottie.api.lifecycle.LifecycleState
import io.kottie.api.lifecycle.PluginLifecycle
import io.kottie.api.plugin.PluginDescription
import io.kottie.api.plugin.PluginState
import io.kottie.api.scoreboard.ScoreboardSlot
import io.kottie.api.title.TitleSpec
import io.kottie.core.KottieRuntime
import io.kottie.core.scoreboard.InMemoryScoreboardService
import io.kottie.core.title.RecordingTitleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class UiAndPluginLoaderTest {

    @Test
    fun `scoreboard objective show and scores`() {
        val runtime = KottieRuntime.start()
        try {
            val obj = runtime.scoreboards.createObjective("sidebar", "Stats")
            obj.setScore("kills", 3)
            obj.setScore("deaths", 1)
            assertEquals(3, obj.getScore("kills"))
            val player = UUID.randomUUID()
            runtime.scoreboards.show(player, "sidebar", ScoreboardSlot.SIDEBAR)
            val mem = runtime.scoreboards as InMemoryScoreboardService
            assertEquals("sidebar", mem.shownObjective(player, ScoreboardSlot.SIDEBAR))
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }

    @Test
    fun `boss bar viewers and progress clamp`() {
        val runtime = KottieRuntime.start()
        try {
            val bar = runtime.bossBars.create("Boss", progress = 2.0, color = BossBarColor.RED)
            assertEquals(1.0, bar.progress, 1e-9)
            val p = UUID.randomUUID()
            bar.addViewer(p)
            assertTrue(p in bar.viewers())
            bar.progress = -1.0
            assertEquals(0.0, bar.progress, 1e-9)
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }

    @Test
    fun `title service records show`() {
        val runtime = KottieRuntime.start()
        try {
            val p = UUID.randomUUID()
            runtime.titles.show(p, TitleSpec("Hello", "World"))
            val rec = runtime.titles as RecordingTitleService
            assertEquals("Hello", rec.active(p)?.title)
            runtime.titles.clear(p)
            assertNull(rec.active(p))
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }

    @Test
    fun `plugin loader dependency order and lifecycle`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val order = AtomicInteger(0)
            val enabledAt = mutableMapOf<String, Int>()

            fun lifecycle(id: String) = object : PluginLifecycle {
                override val state: LifecycleState get() = LifecycleState.RUNNING
                override val scope: CoroutineScope get() = runtime.scope
                override suspend fun onEnable() {
                    enabledAt[id] = order.incrementAndGet()
                }
                override suspend fun onDisable(timeout: Duration) {}
            }

            runtime.plugins.register(
                PluginDescription(id = "a", name = "A", version = "1", depend = listOf("b")),
                lifecycle("a")
            )
            runtime.plugins.register(
                PluginDescription(id = "b", name = "B", version = "1"),
                lifecycle("b")
            )

            val loadOrder = runtime.plugins.resolveLoadOrder()
            assertEquals(listOf("b", "a"), loadOrder)
            val failed = runtime.plugins.enableAll()
            assertTrue(failed.isEmpty())
            assertTrue(enabledAt["b"]!! < enabledAt["a"]!!)
            assertEquals(PluginState.ENABLED, runtime.plugins.get("a")!!.state)

            runtime.plugins.disableAll()
            assertEquals(PluginState.DISABLED, runtime.plugins.get("b")!!.state)
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `plugin loader detects cycle`() {
        val runtime = KottieRuntime.start()
        try {
            runtime.plugins.register(PluginDescription("x", "X", "1", depend = listOf("y")))
            runtime.plugins.register(PluginDescription("y", "Y", "1", depend = listOf("x")))
            assertThrows(IllegalStateException::class.java) {
                runtime.plugins.resolveLoadOrder()
            }
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }
}
