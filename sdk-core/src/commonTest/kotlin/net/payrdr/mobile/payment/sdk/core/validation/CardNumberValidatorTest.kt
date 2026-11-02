package net.payrdr.mobile.payment.sdk.core.validation

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class CardNumberValidatorTest {
    private val validator = CardNumberValidator()

    @Test
    fun `accepts 16 digit PAN`() {
        val r = validator.validate("4556733604106746")
        r.isValid shouldBe true
        r.errorCode.shouldBeNull()
    }

    @Test
    fun `accepts 19 digit PAN`() {
        validator.validate("4539985984741055997").isValid shouldBe true
    }

    @Test
    fun `rejects empty`() {
        val r = validator.validate("")
        r.isValid shouldBe false
        r.errorCode shouldBe ValidationCodes.required
    }

    @Test
    fun `rejects too short`() {
        val r = validator.validate("455673360410674")
        r.isValid shouldBe false
        r.errorCode shouldBe ValidationCodes.invalid
        r.errorMessage!!.shouldContain("length")
    }

    @Test
    fun `rejects too long`() {
        val r = validator.validate("45399859847410559971")
        r.isValid shouldBe false
        r.errorCode shouldBe ValidationCodes.invalid
    }

    @Test
    fun `rejects non-digits`() {
        val r = validator.validate("IncorrectCardNum")
        r.isValid shouldBe false
        r.errorCode shouldBe ValidationCodes.invalidFormat
    }

    @Test
    fun `rejects Luhn failure`() {
        val r = validator.validate("4556733604106745")
        r.isValid shouldBe false
        r.errorCode shouldBe ValidationCodes.invalid
    }
}
