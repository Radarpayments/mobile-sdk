package net.payrdr.mobile.payment.sdk.payment.ui.components

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import kotlin.test.Test

class CascadeLabelTest {
    @Test
    fun `an amount isolated left to right moves as one word`() {
        cascadeWords("Оплатить ${ltr("1 250,00 €")}") shouldBe listOf("Оплатить", ltr("1 250,00 €"))
    }

    @Test
    fun `plain words split on spaces`() {
        cascadeWords("Оплатить картой ${ltr("•••• 1111")}") shouldBe listOf("Оплатить", "картой", ltr("•••• 1111"))
        cascadeWords("  Готово ") shouldBe listOf("Готово")
    }

    @Test
    fun `the icon goes first and a spinner stands alone`() {
        val lock = PaymentIcons.LockBold
        labelParts("Оплатить ${ltr("5 €")}", icon = lock, loading = false) shouldBe
            listOf(LabelPart.Icon(lock), LabelPart.Word("Оплатить"), LabelPart.Word(ltr("5 €")))
        labelParts("Оплатить", icon = lock, loading = true) shouldBe listOf(LabelPart.Spinner)
    }

    @Test
    fun `old words leave one after another`() {
        leaveFraction(index = 0, elapsedMs = 0f) shouldBe 0f
        leaveFraction(index = 0, elapsedMs = 110f) shouldBe 0.5f
        leaveFraction(index = 1, elapsedMs = 45f) shouldBe 0f
        leaveFraction(index = 1, elapsedMs = 265f) shouldBe 1f
    }

    @Test
    fun `new words start once the old ones are on their way`() {
        arriveFraction(index = 0, elapsedMs = 140f, durationMs = 380) shouldBe 0f
        arriveFraction(index = 0, elapsedMs = 520f, durationMs = 380) shouldBe 1f
        arriveFraction(index = 2, elapsedMs = 280f, durationMs = 380) shouldBe 0f
    }

    @Test
    fun `the change lasts until the last word has arrived`() {
        cascadeDurationMs(leaving = 3, arriving = 1) shouldBe 520
        cascadeDurationMs(leaving = 1, arriving = 3) shouldBe 660
        cascadeDurationMs(leaving = 12, arriving = 1) shouldBe 715
    }

    @Test
    fun `words both labels share carry over in order`() {
        val lock = LabelPart.Icon(PaymentIcons.LockBold)
        val pay = LabelPart.Word("Оплатить")
        val byCard = LabelPart.Word("картой")
        matchParts(listOf(lock, pay, LabelPart.Word(ltr("5 €"))), listOf(pay, byCard, LabelPart.Word(ltr("•••• 1111")))) shouldBe
            listOf(1, -1, -1)
        matchParts(listOf(pay, byCard, LabelPart.Word(ltr("•••• 1111"))), listOf(pay, byCard, LabelPart.Word(ltr("•••• 4242")))) shouldBe
            listOf(0, 1, -1)
        matchParts(listOf(LabelPart.Spinner), listOf(LabelPart.Word("Готово"))) shouldBe listOf(-1)
    }

    @Test
    fun `a change of screen turns the whole label over at once`() {
        val from = listOf(LabelPart.Word("Оплатить"), LabelPart.Word("картой"), LabelPart.Word(ltr("•••• 1111")))
        labelChangeDurationMs(from, listOf(LabelPart.Word("Готово")), LabelMotion.Forward) shouldBe 380
        labelChangeDurationMs(from, listOf(LabelPart.Word("Готово")), LabelMotion.Back) shouldBe 380
    }

    @Test
    fun `in place only the changed words cascade`() {
        val pay = LabelPart.Word("Оплатить")
        labelChangeDurationMs(
            listOf(pay, LabelPart.Word(ltr("•••• 1111"))),
            listOf(pay, LabelPart.Word(ltr("•••• 4242"))),
            LabelMotion.InPlace,
        ) shouldBe 520
    }
}
