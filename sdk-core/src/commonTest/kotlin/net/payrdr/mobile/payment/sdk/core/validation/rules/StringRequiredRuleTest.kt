package net.payrdr.mobile.payment.sdk.core.validation.rules

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StringRequiredRuleTest {
    private val rule = StringRequiredRule(code = "C", message = "M")

    @Test
    fun `non-empty values pass`() {
        rule.validateForError("x").isValid shouldBe true
    }

    @Test
    fun `empty string fails`() {
        rule.validateForError("").isValid shouldBe false
    }

    @Test
    fun `blank string fails`() {
        val r = rule.validateForError("   ")
        r.isValid shouldBe false
        r.errorCode shouldBe "C"
        r.errorMessage shouldBe "M"
    }
}
