package net.payrdr.mobile.payment.sdk.payment.presentation

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringCatalogTest {
    private val tables =
        mapOf(
            "en" to mapOf("pay" to "Pay", "pay_amount" to "Pay %1\$s", "route" to "%1\$s to %2\$s", "english_only" to "Only here"),
            "de" to mapOf("pay" to "Zahlen", "pay_amount" to "%1\$s zahlen", "route" to "%2\$s von %1\$s"),
        )

    private fun catalog(language: String) = StringCatalog.of(language, tables::get)

    @Test fun `text in the language`() {
        catalog("de").text("pay") shouldBe "Zahlen"
    }

    @Test fun `unknown language reads in English`() {
        catalog("ja").text("pay") shouldBe "Pay"
    }

    @Test fun `key the language lacks reads in English`() {
        catalog("de").text("english_only") shouldBe "Only here"
    }

    @Test fun `unknown key reads as the key`() {
        catalog("de").text("nowhere") shouldBe "nowhere"
    }

    @Test fun `format fills the placeholders by their numbers`() {
        catalog("en").format("route", listOf("A", "B")) shouldBe "A to B"
        catalog("de").format("route", listOf("A", "B")) shouldBe "B von A"
    }

    @Test fun `format takes an argument as it is`() {
        catalog("en").format("pay_amount", listOf("$1 \\ %2\$s")) shouldBe "Pay $1 \\ %2\$s"
    }

    @Test fun `placeholder without an argument stays`() {
        catalog("en").format("route", listOf("A")) shouldBe "A to %2\$s"
    }
}
