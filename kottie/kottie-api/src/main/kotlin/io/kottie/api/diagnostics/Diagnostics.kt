package io.kottie.api.diagnostics

/**
 * Structured diagnostic event for lifecycle, cancellation, and error reporting.
 * Consumers (logging, metrics, testkit) can observe these without depending on
 * a concrete logging framework.
 */
data class DiagnosticEvent(
    val category: Category,
    val message: String,
    val throwable: Throwable? = null,
    val attributes: Map<String, String> = emptyMap()
) {
    enum class Category {
        LIFECYCLE,
        SCHEDULER,
        CANCELLATION,
        ERROR,
        SHUTDOWN
    }
}

/**
 * Sink for diagnostic events. The runtime registers a default logging sink;
 * tests can install a capturing sink.
 */
fun interface DiagnosticSink {
    fun report(event: DiagnosticEvent)
}
