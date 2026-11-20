package net.payrdr.mobile.payment.sdk.qa.journal

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.logger.LogEvent
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.test.Test

class JournalTest {
    // 2026-11-18 09:05:07.042 UTC
    private val time = 1_794_992_707_042L

    private fun journal(capacity: Int = 2000) = Journal(capacity) { time }

    private fun Journal.messages() = entries.value.map { it.message }

    @Test
    fun `an SDK event becomes an entry`() {
        val journal = journal()
        journal.log(LogEvent(LogLevel.INFO, "PaymentFlow", "order registered", cause = null))
        journal.entries.value shouldBe listOf(JournalEntry(time, LogLevel.INFO, "PaymentFlow", "order registered"))
    }

    @Test
    fun `a cause is appended with its class and message`() {
        val journal = journal()
        journal.log(LogEvent(LogLevel.ERROR, "Gateway", "request failed", IllegalStateException("timeout")))
        journal.messages() shouldBe listOf("request failed (IllegalStateException: timeout)")
    }

    @Test
    fun `a cause without a message is appended as its class alone`() {
        val journal = journal()
        journal.log(LogEvent(LogLevel.WARNING, "Gateway", "request failed", IllegalStateException()))
        journal.messages() shouldBe listOf("request failed (IllegalStateException)")
    }

    @Test
    fun `an anonymous cause is named by its JVM class name`() {
        val journal = journal()
        val cause = object : RuntimeException("boom") {}
        journal.log(LogEvent(LogLevel.ERROR, "Gateway", "request failed", cause))
        journal.messages() shouldBe listOf("request failed (${cause.javaClass.name}: boom)")
    }

    @Test
    fun `only the last 2000 entries are kept`() {
        val journal = journal()
        repeat(2005) { journal.record(LogLevel.DEBUG, "t", "m$it") }
        val messages = journal.messages()
        messages.size shouldBe 2000
        messages.first() shouldBe "m5"
        messages.last() shouldBe "m2004"
    }

    @Test
    fun `a list read from the journal does not change afterwards`() {
        val journal = journal(capacity = 2)
        journal.record(LogLevel.INFO, "t", "a")
        journal.record(LogLevel.INFO, "t", "b")
        val read = journal.entries.value
        journal.record(LogLevel.INFO, "t", "c")
        journal.clear()
        read.map { it.message } shouldBe listOf("a", "b")
    }

    @Test
    fun `entries logged from many threads are all kept`() {
        val journal = journal()
        val start = CountDownLatch(1)
        val workers =
            List(8) { worker ->
                thread {
                    start.await()
                    repeat(200) { journal.log(LogEvent(LogLevel.DEBUG, "w$worker", "$it", cause = null)) }
                }
            }
        start.countDown()
        workers.forEach { it.join() }
        val entries = journal.entries.value
        entries.size shouldBe 1600
        // Each thread's entries keep the order it logged them in.
        entries.groupBy { it.tag }.values.forEach { own -> own.map { it.message } shouldBe List(200) { "$it" } }
    }

    @Test
    fun `clear empties the journal`() {
        val journal = journal()
        journal.record(LogLevel.INFO, "t", "a")
        journal.clear()
        journal.entries.value shouldBe emptyList()
        journal.record(LogLevel.INFO, "t", "b")
        journal.messages() shouldBe listOf("b")
    }

    @Test
    fun `the filter keeps the level and above and matches the text ignoring case`() {
        val entries =
            listOf(
                JournalEntry(time, LogLevel.DEBUG, "merchant", "→ POST register.do"),
                JournalEntry(time, LogLevel.INFO, "Merchant", "← 200"),
                JournalEntry(time, LogLevel.WARNING, "PaymentFlow", "3DS challenge from MERCHANT"),
                JournalEntry(time, LogLevel.ERROR, "PaymentFlow", "declined"),
            )
        entries.filtered(LogLevel.INFO, "merchant") shouldBe entries.subList(1, 3)
        entries.filtered(LogLevel.DEBUG, "") shouldBe entries
        entries.filtered(LogLevel.ERROR, "") shouldBe entries.subList(3, 4)
        entries.filtered(LogLevel.DEBUG, "nothing") shouldBe emptyList()
    }

    @Test
    fun `a line shows the local time, the level, the tag and the message`() {
        val entry = JournalEntry(time, LogLevel.WARNING, "PaymentFlow", "3DS challenge")
        entry.line(TimeZone.getTimeZone("UTC")) shouldBe "09:05:07.042 WARNING PaymentFlow: 3DS challenge"
        entry.line(TimeZone.getTimeZone("GMT+05:30")) shouldBe "14:35:07.042 WARNING PaymentFlow: 3DS challenge"
        entry.line(TimeZone.getTimeZone("GMT-10:00")) shouldBe "23:05:07.042 WARNING PaymentFlow: 3DS challenge"
    }

    @Test
    fun `a line keeps Latin digits whatever the device locale`() {
        val saved = Locale.getDefault()
        // Egyptian Arabic formats numbers with Arabic-Indic digits by default.
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        try {
            JournalEntry(time, LogLevel.INFO, "t", "m").line(TimeZone.getTimeZone("UTC")) shouldBe "09:05:07.042 INFO t: m"
        } finally {
            Locale.setDefault(saved)
        }
    }
}
