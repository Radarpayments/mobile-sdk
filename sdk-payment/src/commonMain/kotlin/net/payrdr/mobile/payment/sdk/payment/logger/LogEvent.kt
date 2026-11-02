package net.payrdr.mobile.payment.sdk.payment.logger

/**
 * Structured log record emitted by [Logger].
 *
 * @property level severity level
 * @property tag short identifier for the origin (class/subsystem)
 * @property message human-readable log line (English, for engineers)
 * @property cause optional throwable for error/warning events
 */
public data class LogEvent(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val cause: Throwable?,
)
