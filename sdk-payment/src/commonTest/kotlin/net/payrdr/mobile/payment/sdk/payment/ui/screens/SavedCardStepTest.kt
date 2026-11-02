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
}
