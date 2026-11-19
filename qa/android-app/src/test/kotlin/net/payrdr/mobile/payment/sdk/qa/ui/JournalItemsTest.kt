package net.payrdr.mobile.payment.sdk.qa.ui

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import kotlin.test.Test

class JournalItemsTest {
    private val request = gateway(1_000, "→ POST https://dev.bpcbt.com/payment/rest/register.do body")
    private val answer = gateway(5_300, "← 200 {\"orderId\":\"1\"}")
    private val sdk = JournalEntry(2_000, LogLevel.INFO, "Payment", "sheet shown")

    @Test
    fun `a request pairs with the next answer of the gateway`() {
        val items = journalItems(listOf(request, sdk, answer))

        items.size shouldBe 2
        val exchange = items[0].shouldBeInstanceOf<JournalItem.Exchange>()
        exchange.response shouldBe answer
        exchange.method shouldBe "POST"
        exchange.path shouldBe "register.do"
        exchange.host shouldBe "dev.bpcbt.com"
        exchange.requestDetail shouldBe "body"
        exchange.status shouldBe 200
        exchange.responseDetail shouldBe "{\"orderId\":\"1\"}"
        exchange.outcome shouldBe "200 · 4.3 s"
        exchange.failed shouldBe false
        items[1].shouldBeInstanceOf<JournalItem.Line>().source shouldBe "SDK"
    }

    @Test
    fun `the result counts the items the journal screen shows`() {
        val second = gateway(6_000, "→ POST https://dev.bpcbt.com/payment/rest/getOrderStatusExtended.do body")
        val secondAnswer = gateway(6_400, "← 200 {}")

        entriesSummary(listOf(request, answer, second, secondAnswer)) shouldBe "2 entries"
        entriesSummary(listOf(request, answer)) shouldBe "1 entry"
        entriesSummary(listOf(request, sdk, answer)) shouldBe "2 entries"
    }

    @Test
    fun `an answer without a request is a line of its own`() {
        val items = journalItems(listOf(answer, request))

        items[0].shouldBeInstanceOf<JournalItem.Line>().source shouldBe "GW"
        items[1].shouldBeInstanceOf<JournalItem.Exchange>().outcome shouldBe "…"
    }

    @Test
    fun `a missing answer reads as no answer and fails`() {
        val lost = JournalEntry(21_000, LogLevel.WARNING, "merchant", "← timeout")
        val exchange = journalItems(listOf(request, lost)).single().shouldBeInstanceOf<JournalItem.Exchange>()

        exchange.status shouldBe null
        exchange.unanswered shouldBe true
        exchange.failed shouldBe true
        exchange.outcome shouldBe "no answer · 20 s"
        exchange.responseDetail shouldBe "timeout"
    }

    @Test
    fun `two requests each take their own answer`() {
        val second = gateway(6_000, "→ POST https://dev.bpcbt.com/payment/rest/getOrderStatusExtended.do body")
        val secondAnswer = gateway(6_500, "← 500 oops")
        val items = journalItems(listOf(request, answer, second, secondAnswer))

        items.map { (it as JournalItem.Exchange).status } shouldBe listOf(200, 500)
        (items[1] as JournalItem.Exchange).failed shouldBe true
    }

    @Test
    fun `pairing keeps every entry in its order`() {
        val entries = listOf(request, sdk, answer)

        journalItems(entries).flatMap { it.entries } shouldBe listOf(request, answer, sdk)
        journalItems(entries).flatMap { it.entries }.toSet() shouldBe entries.toSet()
    }

    @Test
    fun `the filters split gateway, sdk and errors`() {
        val warning = JournalEntry(3_000, LogLevel.WARNING, "Payment", "slow")
        val items = journalItems(listOf(request, answer, sdk, warning))

        items.shown(JournalFilter.ALL, "").size shouldBe 3
        items.shown(JournalFilter.GATEWAY, "").single().shouldBeInstanceOf<JournalItem.Exchange>()
        items.shown(JournalFilter.SDK, "").size shouldBe 2
        items.shown(JournalFilter.ERRORS, "").single().entries shouldBe listOf(warning)
        items.shown(JournalFilter.ALL, "REGISTER").single().shouldBeInstanceOf<JournalItem.Exchange>()
        items.shown(JournalFilter.ALL, "payment").size shouldBe 3
    }

    @Test
    fun `the qa run is its own source`() {
        journalItems(listOf(JournalEntry(0, LogLevel.INFO, "qa", "launch"))).single().shouldBeInstanceOf<JournalItem.Line>().source shouldBe
            "QA"
    }

    private fun gateway(
        time: Long,
        message: String,
    ) = JournalEntry(time, LogLevel.INFO, "merchant", message)
}
