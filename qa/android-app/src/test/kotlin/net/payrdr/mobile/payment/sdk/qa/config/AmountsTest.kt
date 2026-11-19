package net.payrdr.mobile.payment.sdk.qa.config

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AmountsTest {
    @Test
    fun `minor units read as major units with the currency's digits`() {
        minorToMajor(10_000, 2) shouldBe "100.00"
        minorToMajor(250, 2) shouldBe "2.50"
        minorToMajor(5, 2) shouldBe "0.05"
        minorToMajor(0, 2) shouldBe "0.00"
        minorToMajor(1_500, 3) shouldBe "1.500"
        minorToMajor(1_500, 0) shouldBe "1500"
    }

    @Test
    fun `major units are typed with a point or a comma`() {
        majorToMinor("100", 2) shouldBe 10_000
        majorToMinor("100.00", 2) shouldBe 10_000
        majorToMinor("2.5", 2) shouldBe 250
        majorToMinor("2,50", 2) shouldBe 250
        majorToMinor("10.", 2) shouldBe 1_000
        majorToMinor("0.05", 2) shouldBe 5
        majorToMinor("007", 2) shouldBe 700
        majorToMinor("1500", 0) shouldBe 1_500
    }

    @Test
    fun `other text is not an amount`() {
        listOf("", ".50", "2.505", "1.2.3", "+5", "-5", " 5", "5 ", "5 00", "５", "٥", "1e3", "2.5x").forEach { text ->
            majorToMinor(text, 2) shouldBe null
        }
        majorToMinor("15.0", 0) shouldBe null
    }

    @Test
    fun `an amount beyond eighteen digits is not read`() {
        majorToMinor("9999999999999999.99", 2) shouldBe 999_999_999_999_999_999
        majorToMinor("99999999999999999.99", 2) shouldBe null
    }

    @Test
    fun `the conversion goes both ways`() {
        listOf(0L, 1L, 99L, 100L, 10_000L, 999_999_999_999L).forEach { minor ->
            majorToMinor(minorToMajor(minor, 2), 2) shouldBe minor
        }
    }

    @Test
    fun `every currency of the app has two minor digits and an unknown one gets two`() {
        Currencies.codes.forEach { Currencies.minorDigits(it) shouldBe 2 }
        Currencies.minorDigits("XYZ") shouldBe 2
    }
}
