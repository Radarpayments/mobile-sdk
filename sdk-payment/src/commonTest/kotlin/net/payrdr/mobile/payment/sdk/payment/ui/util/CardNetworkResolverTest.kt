package net.payrdr.mobile.payment.sdk.payment.ui.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CardNetworkResolverTest {
    @Test
    fun `resolves Visa from leading 4`() {
        CardNetworkResolver.resolve("4111111111111111") shouldBe CardNetwork.VISA
        CardNetworkResolver.resolve("4") shouldBe CardNetwork.VISA
        CardNetworkResolver.resolve("4539 1488 0343 6467") shouldBe CardNetwork.VISA
    }

    @Test
    fun `resolves Mastercard from 51-55 and 2221-2720 ranges`() {
        CardNetworkResolver.resolve("5100000000000000") shouldBe CardNetwork.MASTERCARD
        CardNetworkResolver.resolve("5599999999999999") shouldBe CardNetwork.MASTERCARD
        CardNetworkResolver.resolve("2221000000000000") shouldBe CardNetwork.MASTERCARD
        CardNetworkResolver.resolve("2720999999999999") shouldBe CardNetwork.MASTERCARD
    }

    @Test
    fun `resolves Amex from 34 or 37`() {
        CardNetworkResolver.resolve("340000000000000") shouldBe CardNetwork.AMEX
        CardNetworkResolver.resolve("370000000000000") shouldBe CardNetwork.AMEX
    }

    @Test
    fun `resolves UnionPay from 62`() {
        CardNetworkResolver.resolve("6200000000000000") shouldBe CardNetwork.UNIONPAY
    }

    @Test
    fun `resolves JCB from 3528-3589`() {
        CardNetworkResolver.resolve("3528000000000000") shouldBe CardNetwork.JCB
        CardNetworkResolver.resolve("3589999999999999") shouldBe CardNetwork.JCB
        CardNetworkResolver.resolve("3527000000000000") shouldBe CardNetwork.UNKNOWN
    }

    @Test
    fun `resolves Diners Club from 36 or 38-39 or 300-305`() {
        CardNetworkResolver.resolve("36000000000000") shouldBe CardNetwork.DINERS
        CardNetworkResolver.resolve("30500000000000") shouldBe CardNetwork.DINERS
        CardNetworkResolver.resolve("3800000000000000") shouldBe CardNetwork.DINERS
        CardNetworkResolver.resolve("3060000000000000") shouldBe CardNetwork.UNKNOWN
    }

    @Test
    fun `resolves Discover from 6011 or 644-649 or 65`() {
        CardNetworkResolver.resolve("6011000000000000") shouldBe CardNetwork.DISCOVER
        CardNetworkResolver.resolve("6440000000000000") shouldBe CardNetwork.DISCOVER
        CardNetworkResolver.resolve("6500000000000000") shouldBe CardNetwork.DISCOVER
    }

    @Test
    fun `resolves Maestro from 50 or 56-58 or 6304 or 6759-6763`() {
        CardNetworkResolver.resolve("5018000000000000") shouldBe CardNetwork.MAESTRO
        CardNetworkResolver.resolve("5600000000000000") shouldBe CardNetwork.MAESTRO
        CardNetworkResolver.resolve("6304000000000000") shouldBe CardNetwork.MAESTRO
        CardNetworkResolver.resolve("6761000000000000") shouldBe CardNetwork.MAESTRO
    }

    @Test
    fun `UnionPay keeps 62 although Discover and Maestro also start with 6`() {
        CardNetworkResolver.resolve("6221260000000000") shouldBe CardNetwork.UNIONPAY
    }

    @Test
    fun `saved cards take the new networks from the gateway name`() {
        CardNetworkResolver.resolveSaved("MAESTRO", "501800**0000") shouldBe CardNetwork.MAESTRO
        CardNetworkResolver.resolveSaved("JCB", "352800**0000") shouldBe CardNetwork.JCB
        CardNetworkResolver.resolveSaved("DISCOVER", "601100**0000") shouldBe CardNetwork.DISCOVER
        CardNetworkResolver.resolveSaved("DINERS", "360000**0000") shouldBe CardNetwork.DINERS
    }

    @Test
    fun `resolves Unknown for empty or unmatched prefixes`() {
        CardNetworkResolver.resolve("") shouldBe CardNetwork.UNKNOWN
        CardNetworkResolver.resolve("1") shouldBe CardNetwork.UNKNOWN
        CardNetworkResolver.resolve("9999999999999999") shouldBe CardNetwork.UNKNOWN
    }

    @Test
    fun `ignores spaces and dashes in the input`() {
        CardNetworkResolver.resolve("4111-1111-1111-1111") shouldBe CardNetwork.VISA
        CardNetworkResolver.resolve("  5111 2222 3333 4444 ") shouldBe CardNetwork.MASTERCARD
    }

    @Test
    fun `resolves Mastercard range edge cases`() {
        CardNetworkResolver.resolve("2220000000000000") shouldBe CardNetwork.UNKNOWN
        CardNetworkResolver.resolve("2721000000000000") shouldBe CardNetwork.UNKNOWN
    }
}
