package net.payrdr.mobile.payment.sdk.payment.ui.util

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlin.test.Test

class CardInputValidationTest {
    @Test
    fun `pan errors are specific`() {
        CardInputValidation.pan("") shouldBe FieldError.PAN_EMPTY
        CardInputValidation.pan("411111") shouldBe FieldError.PAN_SHORT
        CardInputValidation.pan("4111111111111112") shouldBe FieldError.PAN_INVALID
        CardInputValidation.pan("4111111111111111") shouldBe null
    }

    @Test
    fun `pan length follows the network`() {
        CardInputValidation.pan("4111111111111111110") shouldBe null // Visa, 19 digits
        CardInputValidation.pan("41111111111111113") shouldBe FieldError.PAN_SHORT // Visa does not issue 17
        CardFormatters.maxPanLength(CardNetwork.VISA) shouldBe 19
        CardFormatters.maxPanLength(CardNetwork.MASTERCARD) shouldBe 16
        CardFormatters.maxPanLength(CardNetwork.UNKNOWN) shouldBe 19
        CardFormatters.panLengths(CardNetwork.DINERS) shouldBe (14..19).toList()
        CardFormatters.panLengths(CardNetwork.MAESTRO) shouldBe (12..19).toList()
        CardFormatters.panLengths(CardNetwork.JCB) shouldBe (16..19).toList()
    }

    @Test
    fun `holder follows the printed name rules`() {
        CardInputValidation.holder("") shouldBe FieldError.HOLDER_EMPTY
        CardInputValidation.holder("   ") shouldBe FieldError.HOLDER_EMPTY
        CardInputValidation.holder("JOHN123") shouldBe FieldError.HOLDER_FORMAT
        CardInputValidation.holder("ГЕОРГИ ПЕТРОВ") shouldBe FieldError.HOLDER_FORMAT
        CardInputValidation.holder("-.") shouldBe FieldError.HOLDER_FORMAT
        CardInputValidation.holder("A") shouldBe FieldError.HOLDER_SHORT
        CardInputValidation.holder("JOHN SMITH") shouldBe null
        CardInputValidation.holder("MARY-ANN O'NEIL JR.") shouldBe null
        CardInputValidation.holder("ACME") shouldBe null
    }

    @Test
    fun `expiry errors are specific`() {
        val today = LocalDate(2026, 10, 15)
        CardInputValidation.expiry("12", today) shouldBe FieldError.EXPIRY_FORMAT
        CardInputValidation.expiry("1399", today) shouldBe FieldError.EXPIRY_MONTH
        CardInputValidation.expiry("0120", today) shouldBe FieldError.EXPIRY_PAST
        CardInputValidation.expiry("0926", today) shouldBe FieldError.EXPIRY_PAST
        CardInputValidation.expiry("1026", today) shouldBe null
        CardInputValidation.expiry("1299", today) shouldBe null
    }

    @Test
    fun `cvv length depends on network`() {
        CardInputValidation.cvv("12", CardNetwork.VISA) shouldBe FieldError.CVV_3
        CardInputValidation.cvv("123", CardNetwork.VISA) shouldBe null
        CardInputValidation.cvv("123", CardNetwork.AMEX) shouldBe FieldError.CVV_4
    }

    @Test
    fun `email`() {
        CardInputValidation.email(" ") shouldBe FieldError.EMAIL_EMPTY
        CardInputValidation.email("name@example") shouldBe FieldError.EMAIL_INVALID
        CardInputValidation.email("name@example.com") shouldBe null
    }

    @Test
    fun `saved card network prefers payment system name`() {
        CardNetworkResolver.resolveSaved("MASTERCARD", "411111**1111") shouldBe CardNetwork.MASTERCARD
        CardNetworkResolver.resolveSaved("", "411111**1111") shouldBe CardNetwork.VISA
    }

    @Test
    fun `nineteen digit pans keep the tail group`() {
        CardFormatters.formatPan("6212123412341234123") shouldBe "6212 1234 1234 1234 123"
        CardFormatters.shortMask("411111**1111") shouldBe "•••• 1111"
    }
}
