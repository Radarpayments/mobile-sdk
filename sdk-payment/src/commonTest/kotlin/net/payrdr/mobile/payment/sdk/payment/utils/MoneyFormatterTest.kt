package net.payrdr.mobile.payment.sdk.payment.utils

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MoneyFormatterTest {
    @Test
    fun `minor units with prefix currency`() {
        MoneyFormatter.format("4990", "EUR") shouldBe "€49.90"
        MoneyFormatter.format("123456789", "usd") shouldBe "$1,234,567.89"
    }

    @Test
    fun `the fraction is always shown in full`() {
        MoneyFormatter.format("100", "EUR") shouldBe "€1.00"
        MoneyFormatter.format("134", "EUR") shouldBe "€1.34"
        MoneyFormatter.format("150", "EUR") shouldBe "€1.50"
        MoneyFormatter.format("125000", "TRY") shouldBe "1,250.00 ₺"
    }

    @Test
    fun `minor units with suffix and unknown currency`() {
        MoneyFormatter.format("12550", "TRY") shouldBe "125.50 ₺"
        MoneyFormatter.format("5", "CHF") shouldBe "0.05 CHF"
    }

    @Test
    fun `wallet sheets get major units by the currency's exponent`() {
        MoneyFormatter.majorString(10055, "EUR") shouldBe "100.55"
        MoneyFormatter.majorString(5, "usd") shouldBe "0.05"
        MoneyFormatter.majorString(1500, "JPY") shouldBe "1500"
        MoneyFormatter.majorString(1500, "KWD") shouldBe "1.500"
        MoneyFormatter.majorString(123456789, "BHD") shouldBe "123456.789"
        MoneyFormatter.majorString(0, "EUR") shouldBe "0.00"
    }

    @Test
    fun `currency exponent is respected`() {
        MoneyFormatter.format("1500", "JPY") shouldBe "¥1,500"
        MoneyFormatter.format("1500", "KWD") shouldBe "1.500 KWD"
        MoneyFormatter.format("1000", "KWD") shouldBe "1.000 KWD"
    }

    @Test
    fun `preformatted gateway amounts get the currency sign and keep the fraction`() {
        MoneyFormatter.format("1.00 EUR", "EUR") shouldBe "€1.00"
        MoneyFormatter.format("1.34 EUR", null) shouldBe "€1.34"
        MoneyFormatter.format("49.90 USD", "USD") shouldBe "$49.90"
        MoneyFormatter.format("1,250.00 TRY", "TRY") shouldBe "1,250.00 ₺"
        MoneyFormatter.format("1 250,50 TRY", "TRY") shouldBe "1,250.50 ₺"
        MoneyFormatter.format("49.90", "GBP") shouldBe "£49.90"
    }

    @Test
    fun `a gateway amount without a code takes the given currency`() {
        MoneyFormatter.format("329.00", "EUR", "ru") shouldBe "329,00\u00A0€"
        MoneyFormatter.format("329.00", null) shouldBe "329.00"
    }

    @Test
    fun `thousands separator is not mistaken for a fraction`() {
        MoneyFormatter.format("1,000 EUR", "EUR") shouldBe "€1,000.00"
        MoneyFormatter.format("1.000 EUR", "EUR") shouldBe "€1,000.00"
    }

    @Test
    fun `unparsable and blank amounts`() {
        MoneyFormatter.format("about ten", "EUR") shouldBe "about ten"
        MoneyFormatter.format("4990", null) shouldBe "49.90"
        MoneyFormatter.format("  ", "EUR") shouldBe null
        MoneyFormatter.format(null, "EUR") shouldBe null
    }

    @Test
    fun `minor units from raw and preformatted amounts`() {
        MoneyFormatter.minorUnits("4990", "EUR") shouldBe 4990L
        MoneyFormatter.minorUnits("49.90 EUR", null) shouldBe 4990L
        MoneyFormatter.minorUnits("1 250,50 TRY", "TRY") shouldBe 125050L
        MoneyFormatter.minorUnits("1,000 EUR", "EUR") shouldBe 100000L
        MoneyFormatter.minorUnits("10.5 EUR", "EUR") shouldBe 1050L
        MoneyFormatter.minorUnits("1.234,5 EUR", "EUR") shouldBe 123450L
        MoneyFormatter.minorUnits("1.500 KWD", null) shouldBe 1500L
        MoneyFormatter.minorUnits("about ten", "EUR") shouldBe null
        MoneyFormatter.minorUnits(null, "EUR") shouldBe null
    }

    @Test
    fun `the sdk language sets separators and where the currency sign goes`() {
        MoneyFormatter.format("123450", "EUR", "en") shouldBe "€1,234.50"
        MoneyFormatter.format("123450", "EUR", "de") shouldBe "1.234,50\u00A0€"
        MoneyFormatter.format("123450", "EUR", "ro") shouldBe "1.234,50\u00A0€"
        MoneyFormatter.format("123450", "EUR", "el") shouldBe "1.234,50\u00A0€"
        MoneyFormatter.format("123450", "EUR", "ru") shouldBe "1\u00A0234,50\u00A0€"
        MoneyFormatter.format("123450", "EUR", "bg") shouldBe "1\u00A0234,50\u00A0€"
        MoneyFormatter.format("123450", "EUR", "fr") shouldBe "1\u202F234,50\u00A0€"
        MoneyFormatter.format("123450", "EUR", "ar") shouldBe "1,234.50\u00A0€"
    }

    @Test
    fun `spanish groups thousands from five digits`() {
        MoneyFormatter.format("123450", "EUR", "es") shouldBe "1234,50\u00A0€"
        MoneyFormatter.format("1234500", "EUR", "es") shouldBe "12.345,00\u00A0€"
    }

    @Test
    fun `suffix currencies and codes keep their place in any language`() {
        MoneyFormatter.format("125050", "TRY", "de") shouldBe "1.250,50\u00A0₺"
        MoneyFormatter.format("5", "CHF", "fr") shouldBe "0,05\u00A0CHF"
        MoneyFormatter.format("390", "EUR", "it") shouldBe "€3.90"
    }
}
