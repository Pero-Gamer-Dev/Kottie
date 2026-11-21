package io.kottie.bench

/**
 * Tiny timing helper for local microbenchmarks (not JMH).
 * Record JDK, OS, and commit hash when publishing numbers.
 */
object Microbench {

    data class Result(
        val name: String,
        val iterations: Int,
        val totalNanos: Long,
        val opsPerSecond: Double,
        val nanosPerOp: Double
    )

    inline fun measure(name: String, iterations: Int, warmup: Int = iterations / 10, block: () -> Unit): Result {
        repeat(warmup.coerceAtLeast(0)) { block() }
        val start = System.nanoTime()
        repeat(iterations) { block() }
        val total = System.nanoTime() - start
        val ops = iterations * 1_000_000_000.0 / total.coerceAtLeast(1)
        return Result(
            name = name,
            iterations = iterations,
            totalNanos = total,
            opsPerSecond = ops,
            nanosPerOp = total.toDouble() / iterations
        )
    }

    fun format(result: Result): String =
        "%s: %.0f ops/s (%.1f ns/op, n=%d)".format(
            result.name, result.opsPerSecond, result.nanosPerOp, result.iterations
        )
}
