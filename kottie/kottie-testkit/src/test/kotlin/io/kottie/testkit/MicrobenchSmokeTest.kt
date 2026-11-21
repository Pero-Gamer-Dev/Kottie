package io.kottie.testkit

import io.kottie.api.ecs.components.HealthComponent
import io.kottie.bench.Microbench
import io.kottie.ecs.DefaultEntityStore
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MicrobenchSmokeTest {

    @Test
    fun `entity store set get microbench runs`() {
        val store = DefaultEntityStore()
        val entity = store.create()
        val result = Microbench.measure("ecs-set-get", iterations = 5_000, warmup = 500) {
            entity.set(HealthComponent.KEY, HealthComponent(20.0, 20.0))
            entity.get(HealthComponent.KEY)
        }
        println(Microbench.format(result))
        assertTrue(result.opsPerSecond > 0.0)
    }
}
