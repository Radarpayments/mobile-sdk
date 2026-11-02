package net.payrdr.mobile.payment.sdk.core.validation

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CardExpiryValidatorTest {
    private val v = CardExpiryValidator()

    @Test fun `valid MM slash YY`() {
        v.validate("12/28").isValid shouldBe true
    }

    @Test
    fun `required fails on blank`() {
        v.validate("").errorCode shouldBe ValidationCodes.required
    }

    @Test
    fun `wrong format fails`() {
        v.validate("1228").errorCode shouldBe ValidationCodes.invalidFormat
    }

    @Test
    fun `month 13 fails`() {
        v.validate("13/28").errorCode shouldBe ValidationCodes.invalid
    }
}
