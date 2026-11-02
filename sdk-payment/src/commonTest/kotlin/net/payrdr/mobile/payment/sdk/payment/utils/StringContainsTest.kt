package net.payrdr.mobile.payment.sdk.payment.utils

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringContainsTest {
    @Test fun `returns true when any keyword matches ignoring case`() {
        "Order is DEPOSITED now".containsAnyOfKeywordIgnoreCase(listOf(OrderStatus.DEPOSITED, OrderStatus.APPROVED)) shouldBe true
        "approved".containsAnyOfKeywordIgnoreCase(listOf(OrderStatus.APPROVED)) shouldBe true
    }

    @Test fun `returns false when no keyword matches`() {
        "CREATED".containsAnyOfKeywordIgnoreCase(listOf(OrderStatus.DEPOSITED, OrderStatus.APPROVED)) shouldBe false
    }

    @Test fun `empty keyword list returns false`() {
        "DEPOSITED".containsAnyOfKeywordIgnoreCase(emptyList()) shouldBe false
    }
}
