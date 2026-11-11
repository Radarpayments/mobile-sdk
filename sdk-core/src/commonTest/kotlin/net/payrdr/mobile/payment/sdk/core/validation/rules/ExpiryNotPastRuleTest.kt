package net.payrdr.mobile.payment.sdk.core.validation.rules

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlin.test.Test

class ExpiryNotPastRuleTest {
    private val rule = ExpiryNotPastRule(code = "invalid", message = "expired", today = LocalDate(2026, 11, 11))

    @Test
    fun `the current month is not past`() {
        rule.validateForError("11/26").isValid shouldBe true
    }

    @Test
    fun `an earlier month of the same year is past`() {
        rule.validateForError("10/26").errorCode shouldBe "invalid"
    }

    @Test
    fun `an earlier year is past`() {
        rule.validateForError("12/25").errorCode shouldBe "invalid"
    }

    @Test
    fun `a later year with an earlier month is not past`() {
        rule.validateForError("01/27").isValid shouldBe true
    }
}
