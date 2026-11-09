package net.payrdr.mobile.payment.sdk.payment.logger

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LoggerTest {
    private class RecordingTransport : LogTransport {
        val events: MutableList<LogEvent> = mutableListOf()

        override fun log(event: LogEvent) {
            events += event
        }
    }

    @Test
    fun `dispatches event to every registered transport`() {
        val t1 = RecordingTransport()
        val t2 = RecordingTransport()
        val logger = Logger(transports = listOf(t1, t2))

        logger.info(tag = "TAG", message = "hello")

        t1.events shouldHaveSize 1
        t2.events shouldHaveSize 1
        t1.events[0].level shouldBe LogLevel.INFO
        t1.events[0].tag shouldBe "TAG"
        t1.events[0].message shouldBe "hello"
    }

    @Test
    fun `filters debug events when minLevel is INFO`() {
        val transport = RecordingTransport()
        val logger = Logger(transports = listOf(transport), minLevel = LogLevel.INFO)

        logger.debug("TAG", "noise")
        logger.info("TAG", "keeper")

        transport.events shouldHaveSize 1
        transport.events[0].level shouldBe LogLevel.INFO
    }

    @Test
    fun `error carries throwable cause through to transport`() {
        val transport = RecordingTransport()
        val logger = Logger(transports = listOf(transport))
        val cause = IllegalStateException("boom")

        logger.error("TAG", "kaboom", cause)

        transport.events shouldHaveSize 1
        transport.events[0].cause shouldBe cause
    }

    @Test
    fun `NoopLogTransport discards events`() {
        val transport = NoopLogTransport
        // Should not throw, should not do anything observable — smoke test.
        transport.log(LogEvent(LogLevel.ERROR, "TAG", "msg", null))
    }
}
