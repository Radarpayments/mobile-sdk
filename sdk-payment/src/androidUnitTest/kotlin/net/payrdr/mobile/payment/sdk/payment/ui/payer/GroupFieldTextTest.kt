package net.payrdr.mobile.payment.sdk.payment.ui.payer

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GroupFieldTextTest {
    @Test
    fun `the label is the field's name`() {
        groupFieldText(name = "City", mandatory = false, markMandatory = true, format = null) shouldBe
            GroupFieldText(label = "City", hint = "")
    }

    @Test
    fun `a required field among optional ones has an asterisk in its label`() {
        groupFieldText(name = "City", mandatory = true, markMandatory = true, format = null).label shouldBe "City *"
        groupFieldText(name = "City", mandatory = true, markMandatory = false, format = null).label shouldBe "City"
    }

    @Test
    fun `a date shows its format as the hint and keeps it out of the label`() {
        groupFieldText(name = "Date of birth", mandatory = true, markMandatory = true, format = "DD.MM.YYYY") shouldBe
            GroupFieldText(label = "Date of birth *", hint = "DD.MM.YYYY")
    }
}
