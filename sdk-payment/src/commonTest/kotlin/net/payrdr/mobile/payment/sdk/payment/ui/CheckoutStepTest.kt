package net.payrdr.mobile.payment.sdk.payment.ui

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import kotlin.test.Test

class CheckoutStepTest {
    private val card = SavedCardInfo("b1", "411111**1111", "VISA", "JOHN", canDelete = false)
    private val form = PaymentState.CardFormShown(savedCards = listOf(card), allowNewCard = true, cvcRequired = true)
    private val methods = Step.Form(Route.Methods, form, isRoot = true)
    private val newCard = Step.Form(Route.NewCard, form, isRoot = false)
    private val allCards = Step.Form(Route.AllCards, form, isRoot = false)
    private val saved = Step.Form(Route.Saved(card), form, isRoot = false)
    private val processing = Step.Processing(ProcessingStep.SUBMITTING_PAYMENT)
    private val threeDs = Step.ThreeDs
    private val result = Step.Result(PaymentState.Completed(CheckoutResult.Failed(orderId = "p1", error = SdkException())))

    @Test
    fun `forward into the flow and back out of it`() {
        isForward(methods, newCard) shouldBe true
        isForward(methods, saved) shouldBe true
        isForward(allCards, saved) shouldBe true
        isForward(newCard, processing) shouldBe true
        isForward(processing, threeDs) shouldBe true
        isForward(processing, result) shouldBe true
        isForward(threeDs, result) shouldBe true
        isForward(newCard, methods) shouldBe false
        isForward(saved, allCards) shouldBe false
        isForward(result, methods) shouldBe false
        isForward(threeDs, processing) shouldBe false
    }

    @Test
    fun `a step blurs while it changes but not the loading layer under a reveal nor the revealed step`() {
        // Loading → methods: the loading layer stays under the new header, so neither of them blurs.
        Step.Loading.blursWhileChanging(revealKey = methods.key) shouldBe false
        methods.blursWhileChanging(revealKey = methods.key) shouldBe false
        // Any other change: both steps zoom through the light blur, loading included (a failed load shows the result).
        Step.Loading.blursWhileChanging(revealKey = null) shouldBe true
        result.blursWhileChanging(revealKey = null) shouldBe true
        methods.blursWhileChanging(revealKey = null) shouldBe true
        newCard.blursWhileChanging(revealKey = null) shouldBe true
    }

    @Test
    fun `loading and methods and three-ds are separate steps`() {
        Step.Loading.key shouldNotBe methods.key
        threeDs.key shouldNotBe processing.key
    }
}
