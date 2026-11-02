package net.payrdr.mobile.payment.sdk.payment.ui.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CardFormattersTest {
    @Test
    fun `formatPan groups digits into 4-4-4-4 for 16-digit PANs`() {
        CardFormatters.formatPan("4111111111111111") shouldBe "4111 1111 1111 1111"
    }

    @Test
    fun `formatPan groups first 4 then 6 then 5 for Amex 15-digit PANs`() {
        CardFormatters.formatPan("378282246310005") shouldBe "3782 822463 10005"
    }

    @Test
    fun `formatPan ignores non-digits and only formats digits`() {
        CardFormatters.formatPan("4111-1111 1111 1111") shouldBe "4111 1111 1111 1111"
    }

    @Test
    fun `formatPan returns partial groups for in-progress input`() {
        CardFormatters.formatPan("4111") shouldBe "4111"
        CardFormatters.formatPan("41111") shouldBe "4111 1"
        CardFormatters.formatPan("41111111") shouldBe "4111 1111"
    }

    @Test
    fun `formatPan on empty returns empty`() {
        CardFormatters.formatPan("") shouldBe ""
    }

    @Test
    fun `formatExpiry adds slash after two digits`() {
        CardFormatters.formatExpiry("12") shouldBe "12"
        CardFormatters.formatExpiry("123") shouldBe "12/3"
        CardFormatters.formatExpiry("1225") shouldBe "12/25"
    }

    @Test
    fun `formatExpiry ignores non-digits`() {
        CardFormatters.formatExpiry("12/25") shouldBe "12/25"
        CardFormatters.formatExpiry("ab12cd25") shouldBe "12/25"
    }

    @Test
    fun `formatExpiry caps at 4 digits`() {
        CardFormatters.formatExpiry("12252024") shouldBe "12/25"
    }

    @Test
    fun `maskPan shows only last 4 digits preceded by three dot groups`() {
        CardFormatters.maskPan("4111111111111111") shouldBe "•••• •••• •••• 1111"
    }

    @Test
    fun `maskPan handles pans shorter than 4 digits by returning as-is in bullet form`() {
        CardFormatters.maskPan("123") shouldBe "•••"
    }

    @Test
    fun `digitsOnly strips everything non-digit`() {
        CardFormatters.digitsOnly("4111 abc 2222") shouldBe "41112222"
        CardFormatters.digitsOnly("") shouldBe ""
    }

    @Test
    fun `splitBindingLabel separates masked pan and expiry`() {
        CardFormatters.splitBindingLabel("411111**1111 12/26") shouldBe ("411111**1111" to "12/26")
        CardFormatters.splitBindingLabel("411111**1111 12/2026") shouldBe ("411111**1111" to "12/2026")
    }

    @Test
    fun `splitBindingLabel keeps a label without expiry as the masked pan`() {
        CardFormatters.splitBindingLabel("411111**1111") shouldBe ("411111**1111" to null)
        CardFormatters.splitBindingLabel(" 411111**1111 ") shouldBe ("411111**1111" to null)
    }

    @Test
    fun `shortMask of a split label shows the pan last four digits rather than the expiry`() {
        val (maskedPan, _) = CardFormatters.splitBindingLabel("411111**1111 12/26")
        CardFormatters.shortMask(maskedPan) shouldBe "•••• 1111"
    }
}
