package net.payrdr.mobile.payment.sdk.payment.ui

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability
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

    @Test
    fun `the session loads until the form and its wallets are in`() {
        sessionStep(PaymentState.Initializing, emptyList(), WalletAvailability.None, null, methods) shouldBe Step.Loading
        sessionStep(PaymentState.Processing(ProcessingStep.LOADING_SESSION), emptyList(), WalletAvailability.None, null, methods) shouldBe
            Step.Loading
        sessionStep(form, emptyList(), wallets = null, heldResult = null, lastStep = methods) shouldBe Step.Loading
        sessionStep(PaymentState.Processing(ProcessingStep.SUBMITTING_PAYMENT), emptyList(), null, null, methods) shouldBe processing
        sessionStep(PaymentState.ThreeDsChallenge, emptyList(), null, null, methods) shouldBe threeDs
    }

    @Test
    fun `the form opens on its root or on the top of the stack`() {
        sessionStep(form, emptyList(), WalletAvailability.None, null, Step.Loading) shouldBe methods
        sessionStep(form, listOf(Route.Methods, Route.NewCard), WalletAvailability.None, null, Step.Loading) shouldBe newCard
        val noCards = form.copy(savedCards = emptyList())
        sessionStep(noCards, emptyList(), WalletAvailability.None, null, Step.Loading) shouldBe
            Step.Form(Route.NewCard, noCards, isRoot = true)
    }

    @Test
    fun `a completed run shows the held result or keeps the last step`() {
        sessionStep(result.completed, emptyList(), WalletAvailability.None, heldResult = result.completed, lastStep = newCard) shouldBe
            result
        sessionStep(result.completed, emptyList(), WalletAvailability.None, heldResult = null, lastStep = newCard) shouldBe newCard
    }

    @Test
    fun `the first form and a form back from a result start from the root`() {
        stackFor(form, Route.Methods, stack = emptyList(), restart = false) shouldBe listOf(Route.Methods)
        stackFor(form, Route.Methods, stack = listOf(Route.Methods, Route.NewCard), restart = true) shouldBe listOf(Route.Methods)
        stackFor(form, Route.Methods, stack = listOf(Route.Methods, Route.NewCard), restart = false) shouldBe
            listOf(Route.Methods, Route.NewCard)
    }

    @Test
    fun `a removed card leaves the stack`() {
        val gone = Route.Saved(card.copy(bindingId = "b2"))
        stackFor(form, Route.Methods, stack = listOf(Route.Methods, Route.AllCards, gone), restart = false) shouldBe
            listOf(Route.Methods, Route.AllCards)
        stackFor(form, Route.Methods, stack = listOf(gone), restart = false) shouldBe listOf(Route.Methods)
        stackFor(form, Route.Methods, stack = listOf(Route.Methods, Route.Saved(card)), restart = false) shouldBe
            listOf(Route.Methods, Route.Saved(card))
    }
}
