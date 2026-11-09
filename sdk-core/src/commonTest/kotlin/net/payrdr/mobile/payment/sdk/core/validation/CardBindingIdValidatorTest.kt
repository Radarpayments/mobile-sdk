package net.payrdr.mobile.payment.sdk.core.validation

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CardBindingIdValidatorTest {
    private val v = CardBindingIdValidator()

    @Test fun `uuid passes`() {
        v.validate("47eb0336-5ad9-4e03-8a1e-b9f3656ec768").isValid shouldBe true
    }

    @Test fun `blank fails required`() {
        v.validate("").errorCode shouldBe ValidationCodes.required
    }

    @Test fun `contains whitespace fails`() {
        v.validate("abc def").errorCode shouldBe ValidationCodes.invalid
    }
}
