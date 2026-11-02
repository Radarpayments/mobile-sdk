package net.payrdr.mobile.payment.sdk.payment.logger

/**
 * Fan-out logger that dispatches every event to every registered [LogTransport].
 *
 * @param transports list of sinks; events are dispatched in iteration order
 * @param minLevel minimum severity to forward; events below this are dropped before dispatch
 */
public class Logger(
    private val transports: List<LogTransport> = listOf(NoopLogTransport),
    private val minLevel: LogLevel = LogLevel.DEBUG,
) {
    public fun debug(
        tag: String,
        message: String,
    ): Unit = emit(LogLevel.DEBUG, tag, message, cause = null)

    public fun info(
        tag: String,
        message: String,
    ): Unit = emit(LogLevel.INFO, tag, message, cause = null)

    public fun warning(
        tag: String,
        message: String,
        cause: Throwable? = null,
    ): Unit = emit(LogLevel.WARNING, tag, message, cause)

    public fun error(
        tag: String,
        message: String,
        cause: Throwable? = null,
    ): Unit = emit(LogLevel.ERROR, tag, message, cause)

    private fun emit(
        level: LogLevel,
        tag: String,
        message: String,
        cause: Throwable?,
    ) {
        if (level.ordinal < minLevel.ordinal) return
        val event = LogEvent(level, tag, message, cause)
        transports.forEach { it.log(event) }
    }
}
