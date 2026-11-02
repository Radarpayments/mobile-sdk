package net.payrdr.mobile.payment.sdk.core.validation.rules

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ExpiryRuleTest {
    private val rule = ExpiryRule(code = "C", message = "M")

    @Test
    fun `valid MM slash YY passes`() {
        rule.validateForError("12/28").isValid shouldBe true
        rule.validateForError("01/25").isValid shouldBe true
    }

    @Test
    fun `month 00 fails`() {
        rule.validateForError("00/28").isValid shouldBe false
    }

    @Test
    fun `month 13 fails`() {
        rule.validateForError("13/28").isValid shouldBe false
    }

    @Test
    fun `non-numeric parts fail`() {
        rule.validateForError("ab/cd").isValid shouldBe false
    }
}
