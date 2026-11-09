package net.payrdr.mobile.payment.sdk.core.validation

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class OrderNumberValidatorTest {
    private val v = OrderNumberValidator()

    @Test fun `non-blank no whitespace passes`() {
        v.validate("order-001").isValid shouldBe true
    }

    @Test fun `blank fails required`() {
        v.validate("").errorCode shouldBe ValidationCodes.required
    }

    @Test fun `whitespace fails`() {
        v.validate("bad order").errorCode shouldBe ValidationCodes.invalid
    }
}
