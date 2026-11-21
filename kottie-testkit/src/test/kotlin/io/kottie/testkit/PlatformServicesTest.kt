package io.kottie.testkit

import io.kottie.api.command.CommandContext
import io.kottie.api.command.CommandMeta
import io.kottie.api.command.CommandResult
import io.kottie.api.messaging.RecordingMessenger
import io.kottie.api.service.get
import io.kottie.api.service.register
import io.kottie.core.KottieRuntime
import io.kottie.core.permission.DefaultPermissionService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class PlatformServicesTest {

    @Test
    fun `command dispatch with permission`() = runBlocking {
        val runtime = KottieRuntime.start()
        try {
            val perms = runtime.permissions as DefaultPermissionService
            val player = UUID.randomUUID()
            perms.grant(player, "kottie.hello")

            runtime.commands.register(
                CommandMeta(name = "hello", permission = "kottie.hello"),
                executor = { CommandResult.SUCCESS }
            )

            val ok = runtime.commands.dispatch(
                CommandContext(player, "Steve", true, "hello", emptyList(), "hello")
            )
            assertEquals(CommandResult.SUCCESS, ok)

            val denied = runtime.commands.dispatch(
                CommandContext(UUID.randomUUID(), "Alex", true, "hello", emptyList(), "hello")
            )
            assertEquals(CommandResult.NO_PERMISSION, denied)
        } finally {
            runtime.onDisable(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `messenger records broadcast`() {
        val runtime = KottieRuntime.start()
        try {
            val messenger = runtime.messenger as RecordingMessenger
            val id = UUID.randomUUID()
            messenger.send(id, "hi")
            messenger.broadcast("server restart")
            assertEquals(2, messenger.messages.size)
            assertEquals("hi", messenger.messages[0].message)
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }

    @Test
    fun `service registry priority`() {
        val runtime = KottieRuntime.start()
        try {
            interface Marker { val id: String }
            class A : Marker { override val id = "a" }
            class B : Marker { override val id = "b" }
            runtime.services.register<Marker>(A(), priority = 1)
            runtime.services.register<Marker>(B(), priority = 10)
            assertEquals("b", runtime.services.get<Marker>()!!.id)
            assertEquals(2, runtime.services.getAll(Marker::class.java).size)
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }

    @Test
    fun `region scheduler executes`() {
        val runtime = KottieRuntime.start()
        try {
            val ran = AtomicBoolean(false)
            runtime.regions.execute("world", 0, 0) { ran.set(true) }
            // in-memory scheduler runs inline when already "server thread" may differ
            Thread.sleep(50)
            // Fallback schedules onto server executor — allow a moment
            var spins = 0
            while (!ran.get() && spins++ < 20) Thread.sleep(25)
            assertTrue(ran.get())
        } finally {
            runBlocking { runtime.onDisable(Duration.ofSeconds(2)) }
        }
    }
}
