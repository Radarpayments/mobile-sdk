package net.payrdr.mobile.payment.sdk.payment.ui.screens

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import kotlin.test.Test

class SavedCardStepTest {
    private val visa = SavedCardInfo("b1", "411111**1111", "VISA", "", canDelete = false, expiry = "12/26")

    private fun form(
        cvcRequired: Boolean,
        payerData: PayerDataRequest = PayerDataRequest(),
    ) = PaymentState.CardFormShown(savedCards = listOf(visa), allowNewCard = true, cvcRequired = cvcRequired, payerData = payerData)

    @Test
    fun `a card that asks for nothing is paid straight from the sheet`() {
        needsSavedCardScreen(form(cvcRequired = false), visa) shouldBe false
    }

    @Test
    fun `a CVV or payer fields for the card's network open the card step`() {
        needsSavedCardScreen(form(cvcRequired = true), visa) shouldBe true
        val email = PayerDataRequest(fieldsByPaymentSystem = mapOf("VISA" to listOf(PayerFieldSpec(PayerField.EMAIL, mandatory = true))))
        needsSavedCardScreen(form(cvcRequired = false, payerData = email), visa) shouldBe true
    }

    @Test
    fun `payer fields asked for another network do not`() {
        val other = PayerDataRequest(fieldsByPaymentSystem = mapOf("AMEX" to listOf(PayerFieldSpec(PayerField.EMAIL, mandatory = true))))
        needsSavedCardScreen(form(cvcRequired = false, payerData = other), visa) shouldBe false
    }

    @Test
    fun `a saved card is looked up under every name the gateway may give its network`() {
        savedCardPaymentSystems(visa.copy(paymentSystem = "maestro")) shouldBe listOf("MAESTRO", "MASTERCARD")
        savedCardPaymentSystems(visa.copy(paymentSystem = "CUP")) shouldBe listOf("CUP", "UNIONPAY")
        savedCardPaymentSystems(visa.copy(paymentSystem = "DINERS")) shouldBe listOf("DINERS", "DINERS_CLUB")
    }

    @Test
    fun `a card without a payment system is looked up by its number`() {
        savedCardPaymentSystems(visa.copy(paymentSystem = "")) shouldBe listOf("VISA")
        savedCardPaymentSystems(visa.copy(paymentSystem = " ", maskedPan = "000000**0000")) shouldBe emptyList()
    }

    @Test
    fun `a saved card's row names its network and expiry`() {
        visa.rowSubtitle() shouldBe "Visa · 12/26"
        visa.copy(paymentSystem = "AMEX", expiry = null).rowSubtitle() shouldBe "American Express"
        visa.copy(paymentSystem = "LOCALCARD", maskedPan = "000000**0000").rowSubtitle() shouldBe "LOCALCARD · 12/26"
        visa.copy(paymentSystem = "", maskedPan = "000000**0000", expiry = null).rowSubtitle() shouldBe null
    }
}
