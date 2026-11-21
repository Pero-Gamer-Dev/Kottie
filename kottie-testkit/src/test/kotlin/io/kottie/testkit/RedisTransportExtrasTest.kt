package io.kottie.testkit

import io.kottie.network.InMemoryRedisTransport
import io.kottie.network.NamespacedRedisTransport
import io.kottie.network.ReconnectableRedisTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class RedisTransportExtrasTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @AfterEach
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `namespaced transport prefixes channels`() = runBlocking {
        val inner = InMemoryRedisTransport()
        inner.connect()
        val namespaced = NamespacedRedisTransport(inner, "prod")
        val got = CopyOnWriteArrayList<String>()
        // Subscribe on physical channel via inner to prove prefix
        inner.subscribe("prod:chat") { got.add(it.decodeToString()) }
        namespaced.publish("chat", "hi".encodeToByteArray())
        delay(30)
        assertEquals(listOf("hi"), got.toList())
        assertEquals("prod:chat", namespaced.physicalChannel("chat"))
        namespaced.disconnect()
    }

    @Test
    fun `reconnectable rebinds subscriptions after drop`() = runBlocking {
        val inner = InMemoryRedisTransport()
        val transport = ReconnectableRedisTransport(
            inner, scope,
            initialDelay = Duration.ofMillis(20),
            maxDelay = Duration.ofMillis(100)
        )
        transport.connect()
        val count = AtomicInteger(0)
        transport.subscribe("c") { count.incrementAndGet() }
        transport.publish("c", "a".encodeToByteArray())
        delay(30)
        assertEquals(1, count.get())

        inner.simulateDrop()
        transport.notifyDisconnected()
        delay(150)
        // After reconnect path, publish again
        if (!transport.isConnected) {
            runCatching { transport.connect() }
        }
        transport.publish("c", "b".encodeToByteArray())
        delay(50)
        assertTrue(count.get() >= 2, "expected rebind to deliver, count=${count.get()}")
        transport.disconnect()
    }
}
