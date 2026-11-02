package net.payrdr.mobile.payment.sdk.core.validation.rules

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LuhnStringRuleTest {
    private val rule = LuhnStringRule(code = "CODE", message = "MSG")

    @Test
    fun `accepts valid PANs`() {
        rule.validateForError("4532896701439077").isValid shouldBe true
        rule.validateForError("5555555555554444").isValid shouldBe true
        rule.validateForError("378282246310005").isValid shouldBe true
    }

    @Test
    fun `rejects invalid PAN with provided code and message`() {
        val result = rule.validateForError("4532896701439078")
        result.isValid shouldBe false
        result.errorCode shouldBe "CODE"
        result.errorMessage shouldBe "MSG"
    }

    @Test
    fun `empty string passes since sum 0 mod 10 is 0`() {
        rule.validateForError("").isValid shouldBe true
    }
}
