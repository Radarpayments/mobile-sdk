package net.payrdr.mobile.payment.sdk.payment.ui.components

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GroupFieldLabelTest {
    @Test
    fun `the label rests in an empty field without focus`() {
        groupLabelRaised(focused = false, value = "") shouldBe false
    }

    @Test
    fun `the label rises with focus or a value`() {
        groupLabelRaised(focused = true, value = "") shouldBe true
        groupLabelRaised(focused = false, value = "Limassol") shouldBe true
        groupLabelRaised(focused = true, value = "Limassol") shouldBe true
    }

    @Test
    fun `a resting label looks like a hint`() {
        groupLabelTone(raised = false, focused = false, isError = false) shouldBe GroupLabelTone.HINT
    }

    @Test
    fun `a raised label names the field and takes the outline colour while focused`() {
        groupLabelTone(raised = true, focused = false, isError = false) shouldBe GroupLabelTone.NAME
        groupLabelTone(raised = true, focused = true, isError = false) shouldBe GroupLabelTone.FOCUS
    }

    @Test
    fun `an error colours the label wherever it is`() {
        groupLabelTone(raised = false, focused = false, isError = true) shouldBe GroupLabelTone.ERROR
        groupLabelTone(raised = true, focused = true, isError = true) shouldBe GroupLabelTone.ERROR
    }
}
