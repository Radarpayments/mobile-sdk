package net.payrdr.mobile.payment.sdk.core.validation

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PubKeyValidatorTest {
    private val v = PubKeyValidator()

    @Test fun `any non-blank passes`() {
        v.validate("abc").isValid shouldBe true
    }

    @Test fun `blank fails required`() {
        v.validate("").errorCode shouldBe ValidationCodes.required
    }
}
