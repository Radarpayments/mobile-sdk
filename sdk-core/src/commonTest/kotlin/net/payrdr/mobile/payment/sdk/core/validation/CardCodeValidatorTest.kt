package net.payrdr.mobile.payment.sdk.core.validation

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CardCodeValidatorTest {
    private val v = CardCodeValidator()

    @Test fun `three digits pass`() {
        v.validate("123").isValid shouldBe true
    }

    @Test
    fun `empty fails required`() {
        v.validate("").errorCode shouldBe ValidationCodes.required
    }

    @Test
    fun `two digits fail length`() {
        v.validate("12").errorCode shouldBe ValidationCodes.invalid
    }

    @Test
    fun `letters fail format`() {
        v.validate("12a").errorCode shouldBe ValidationCodes.invalid
    }
}
