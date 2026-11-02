package net.payrdr.mobile.payment.sdk.payment.ui.state

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import kotlin.test.Test

class PaymentCheckoutStateReducerTest {
    @Test
    fun `Initializing maps to ShowProcessing with null step`() {
        reduceUiAction(prev = null, next = PaymentState.Initializing) shouldBe
            CheckoutUiAction.ShowProcessing(step = null)
    }

    @Test
    fun `Processing maps to ShowProcessing carrying its step`() {
        reduceUiAction(prev = null, next = PaymentState.Processing(ProcessingStep.LOADING_SESSION)) shouldBe
            CheckoutUiAction.ShowProcessing(step = ProcessingStep.LOADING_SESSION)
    }

    @Test
    fun `CardFormShown with no saved cards maps to ShowCardForm`() {
        val state =
            PaymentState.CardFormShown(
                savedCards = emptyList(),
                allowNewCard = true,
                cvcRequired = true,
            )
        reduceUiAction(prev = null, next = state) shouldBe
            CheckoutUiAction.ShowCardForm(
                savedCards = emptyList(),
                cvcRequired = true,
                allowNewCard = true,
            )
    }

    @Test
    fun `CardFormShown with saved cards maps to ShowCardList`() {
        val saved =
            listOf(
                SavedCardInfo("b1", "•••• 1111", "VISA", "HOLDER", canDelete = true),
            )
        val state =
            PaymentState.CardFormShown(
                savedCards = saved,
                allowNewCard = true,
                cvcRequired = false,
            )
        reduceUiAction(prev = null, next = state) shouldBe
            CheckoutUiAction.ShowCardList(
                savedCards = saved,
                cvcRequired = false,
                allowNewCard = true,
            )
    }

    @Test
    fun `ThreeDsChallenge maps to ShowThreeDs`() {
        reduceUiAction(prev = null, next = PaymentState.ThreeDsChallenge) shouldBe
            CheckoutUiAction.ShowThreeDs
    }

    @Test
    fun `Completed successful maps to ShowResult success`() {
        val result = CheckoutResult.Completed(orderId = "p1")
        reduceUiAction(prev = null, next = PaymentState.Completed(result)) shouldBe
            CheckoutUiAction.ShowResult(result)
    }

    @Test
    fun `Completed failed maps to ShowResult carrying the error`() {
        val result = CheckoutResult.Failed(orderId = "p1", error = SdkDeclinedException(message = "Declined"))
        reduceUiAction(prev = null, next = PaymentState.Completed(result)) shouldBe
            CheckoutUiAction.ShowResult(result)
    }
}
