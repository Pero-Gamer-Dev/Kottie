package io.kottie.testkit

import io.kottie.adapter.paper.PaperServerAdapter
import io.kottie.adapter.paper.UnsupportedServerEnvironmentException
import io.kottie.core.scheduler.InMemoryServerScheduler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PaperAdapterTest {

    @Test
    fun `paper adapter fails fast without paper binding`() {
        val adapter = PaperServerAdapter(paperAvailable = false)
        assertThrows(UnsupportedServerEnvironmentException::class.java) {
            adapter.initialize()
        }
    }

    @Test
    fun `paper adapter initializes when scheduler provided`() {
        val scheduler = InMemoryServerScheduler()
        val adapter = PaperServerAdapter(
            paperAvailable = false,
            schedulerOverride = scheduler
        )
        adapter.initialize()
        assertEquals("paper", adapter.serverName)
        assertTrue(adapter.supportedVersions.isNotEmpty())
        adapter.shutdown()
        scheduler.shutdown()
    }
}
