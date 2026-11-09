package net.payrdr.mobile.payment.sdk.core.validation.rules

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RegExRuleTest {
    private val digitsOnly = RegExRule(code = "C", message = "M", regex = Regex("[0-9]+"))

    @Test
    fun `matches are valid`() {
        digitsOnly.validateForError("12345").isValid shouldBe true
    }

    @Test
    fun `non-matches fail with code and message`() {
        val r = digitsOnly.validateForError("12a45")
        r.isValid shouldBe false
        r.errorCode shouldBe "C"
        r.errorMessage shouldBe "M"
    }
}
