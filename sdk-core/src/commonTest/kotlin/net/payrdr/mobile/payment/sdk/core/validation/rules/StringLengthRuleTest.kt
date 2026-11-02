package net.payrdr.mobile.payment.sdk.core.validation.rules

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringLengthRuleTest {
    private val rule = StringLengthRule(code = "C", message = "M", minLength = 3, maxLength = 5)

    @Test
    fun `accepts length in range`() {
        rule.validateForError("abc").isValid shouldBe true
        rule.validateForError("abcde").isValid shouldBe true
    }

    @Test
    fun `rejects too short`() {
        val r = rule.validateForError("ab")
        r.isValid shouldBe false
        r.errorCode shouldBe "C"
        r.errorMessage shouldBe "M"
    }

    @Test
    fun `rejects too long`() {
        rule.validateForError("abcdef").isValid shouldBe false
    }
}
