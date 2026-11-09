package net.payrdr.mobile.payment.sdk.core.validation

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CardHolderValidatorTest {
    private val v = CardHolderValidator()

    @Test fun `ascii letters and spaces pass`() {
        v.validate("JOHN SMITH").isValid shouldBe true
    }

    @Test fun `hyphens apostrophes and dots pass - as on the gateway's payment page`() {
        v.validate("MARY-ANN O'NEIL JR.").isValid shouldBe true
    }

    @Test
    fun `digits fail format`() {
        v.validate("JOHN123").errorCode shouldBe ValidationCodes.invalidFormat
    }

    @Test
    fun `punctuation without a letter fails format`() {
        v.validate("-.").errorCode shouldBe ValidationCodes.invalidFormat
    }

    @Test
    fun `empty fails required`() {
        v.validate("").errorCode shouldBe ValidationCodes.required
    }

    @Test
    fun `non-ascii chars fail format`() {
        v.validate("ГЕОРГИ").errorCode shouldBe ValidationCodes.invalidFormat
    }

    @Test
    fun `over 30 chars fails length`() {
        v.validate("A".repeat(31)).errorCode shouldBe ValidationCodes.invalid
    }
}
