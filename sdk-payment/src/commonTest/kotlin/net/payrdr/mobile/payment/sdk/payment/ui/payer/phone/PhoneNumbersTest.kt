package net.payrdr.mobile.payment.sdk.payment.ui.payer.phone

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PhoneNumbersTest {
    private val gb = PhoneCountries.byIso("GB")!!
    private val us = PhoneCountries.byIso("US")!!
    private val ru = PhoneCountries.byIso("RU")!!
    private val it = PhoneCountries.byIso("IT")!!

    @Test
    fun `table covers the gateway countries with sane data`() {
        (PhoneCountries.all.size > 200) shouldBe true
        gb.dialCode shouldBe "44"
        gb.lengths shouldBe listOf(10)
        PhoneCountries.all.all { c -> c.lengths.isNotEmpty() && c.example.length in c.lengths } shouldBe true
    }

    @Test
    fun `shared calling codes resolve to the main or preferred country`() {
        PhoneCountries.detect("12015550123")?.first?.iso shouldBe "US"
        PhoneCountries.detect("15062345678", preferred = "CA")?.first?.iso shouldBe "CA"
        PhoneCountries.detect("79123456789")?.first?.iso shouldBe "RU"
        PhoneCountries.detect("35796123456")?.first?.iso shouldBe "CY"
    }

    @Test
    fun `formats as typed with the country grouping`() {
        PhoneNumbers.format("7700900123", gb) shouldBe "7700 900123"
        PhoneNumbers.format("77009", gb) shouldBe "7700 9"
        PhoneNumbers.format("2015550123", us) shouldBe "201 555 0123"
        PhoneNumbers.placeholder(gb) shouldBe "7400 123456"
        PhoneNumbers.format("123", null) shouldBe "123"
    }

    @Test
    fun `national prefix is dropped only where numbers never start with it`() {
        PhoneNumbers.national("07700900123", gb) shouldBe "7700900123"
        PhoneNumbers.national("89123456789", ru) shouldBe "9123456789"
        PhoneNumbers.national("3123456789", it) shouldBe "3123456789"
        PhoneNumbers.national("77009001234567", gb) shouldBe "7700900123"
    }

    @Test
    fun `international input switches country`() {
        PhoneNumbers.input("+1 (201) 555-0123", gb).let { (c, n) -> c?.iso to n } shouldBe ("US" to "2015550123")
        PhoneNumbers.input("0044 7700 900123", us).let { (c, n) -> c?.iso to n } shouldBe ("GB" to "7700900123")
        PhoneNumbers.input("7700 900123", gb).let { (c, n) -> c?.iso to n } shouldBe ("GB" to "7700900123")
    }

    @Test
    fun `e164 and parse round trip`() {
        PhoneNumbers.e164(gb, "7700900123") shouldBe "+447700900123"
        PhoneNumbers.e164(gb, "") shouldBe ""
        PhoneNumbers.parse("+447700900123")?.let { (c, n) -> c.iso to n } shouldBe ("GB" to "7700900123")
    }
}
