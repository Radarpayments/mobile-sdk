package net.payrdr.mobile.payment.sdk.core.validation.rules

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class IntRangeRuleTest {
    private val rule = IntRangeRule(code = "C", message = "M", min = 1, max = 12)

    @Test
    fun `value in range is valid`() {
        rule.validateForError(1).isValid shouldBe true
        rule.validateForError(12).isValid shouldBe true
    }

    @Test
    fun `value below min fails`() {
        rule.validateForError(0).isValid shouldBe false
    }

    @Test
    fun `value above max fails`() {
        rule.validateForError(13).isValid shouldBe false
    }
}
