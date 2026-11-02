package net.payrdr.mobile.payment.sdk.payment.logger

/**
 * Sink for log events. Platform-specific transports (Sentry, OSLog, etc.) implement this.
 * Any transport implementation must be thread-safe — events may arrive from many coroutines.
 */
public fun interface LogTransport {
    public fun log(event: LogEvent)
}
