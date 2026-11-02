package net.payrdr.mobile.payment.sdk.payment.logger

/**
 * Prints events to the platform default standard output (`println`). Useful for debug builds.
 */
public object ConsoleLogTransport : LogTransport {
    override fun log(event: LogEvent) {
        val line = "[${event.level}] ${event.tag}: ${event.message}"
        println(line)
        event.cause?.printStackTrace()
    }
}
