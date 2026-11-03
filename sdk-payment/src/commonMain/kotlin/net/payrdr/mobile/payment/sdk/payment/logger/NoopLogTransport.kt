package net.payrdr.mobile.payment.sdk.payment.logger

/**
 * Discards all events. Default transport when the consumer does not provide one.
 */
internal object NoopLogTransport : LogTransport {
    override fun log(event: LogEvent) = Unit
}
