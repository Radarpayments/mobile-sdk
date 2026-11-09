package net.payrdr.mobile.payment.sdk.payment.ui.payer

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import kotlin.test.Test

class TransferFieldsTest {
    @Test fun `a birthday goes to the screen as DD MM YYYY with dots and back to the gateway as YYYYMMDD`() {
        TransferFields.dateForInput("19901212") shouldBe "12.12.1990"
        TransferFields.dateForApi("03.07.1985") shouldBe "19850703"
        TransferFields.dateForApi("3.7.85") shouldBe "3.7.85"
    }

    @Test fun `typed date digits get their dots`() {
        TransferFields.formatDateInput("1", TODAY) shouldBe "1"
        TransferFields.formatDateInput("120", TODAY) shouldBe "12.0"
        TransferFields.formatDateInput("12.03.1990", TODAY) shouldBe "12.03.1990"
        TransferFields.formatDateInput("120319901", TODAY) shouldBe "12.03.1990"
    }

    @Test fun `a day that no month has is not taken`() {
        TransferFields.formatDateInput("32", TODAY) shouldBe "3"
        TransferFields.formatDateInput("00", TODAY) shouldBe "0"
    }

    @Test fun `a digit that can only start a single-digit day or month gets its zero`() {
        TransferFields.formatDateInput("4", TODAY) shouldBe "04"
        TransferFields.formatDateInput("122", TODAY) shouldBe "12.02"
    }

    @Test fun `a month without that day is not taken`() {
        TransferFields.formatDateInput("1213", TODAY) shouldBe "12.1"
        TransferFields.formatDateInput("3104", TODAY) shouldBe "31.0"
        TransferFields.formatDateInput("3002", TODAY) shouldBe "30.0"
        TransferFields.formatDateInput("2902", TODAY) shouldBe "29.02"
    }

    @Test fun `a year that makes no such day or that is out of range is not taken`() {
        TransferFields.formatDateInput("29022023", TODAY) shouldBe "29.02.202"
        TransferFields.formatDateInput("29022024", TODAY) shouldBe "29.02.2024"
        TransferFields.formatDateInput("010118", TODAY) shouldBe "01.01.1"
        TransferFields.formatDateInput("0101199999", TODAY) shouldBe "01.01.1999"
        TransferFields.formatDateInput("01019", TODAY) shouldBe "01.01"
    }

    @Test fun `a birthday cannot be in the future`() {
        TransferFields.formatDateInput("31102026", TODAY) shouldBe "31.10.202"
        TransferFields.formatDateInput("30102026", TODAY) shouldBe "30.10.2026"
    }

    @Test fun `a separator after one digit closes the day or the month`() {
        TransferFields.formatDateInput("1.", TODAY) shouldBe "01"
        TransferFields.formatDateInput("1.2.", TODAY) shouldBe "01.02"
        TransferFields.formatDateInput("1/2/1990", TODAY) shouldBe "01.02.1990"
        TransferFields.formatDateInput("5-6-1990", TODAY) shouldBe "05.06.1990"
    }

    @Test fun `a date is a real past day`() {
        TransferFields.isValidDate("29.02.2024", TODAY) shouldBe true
        TransferFields.isValidDate("29.02.2023", TODAY) shouldBe false
        TransferFields.isValidDate("31.04.1990", TODAY) shouldBe false
        TransferFields.isValidDate("01.01.2030", TODAY) shouldBe false
        TransferFields.isValidDate("12.1990", TODAY) shouldBe false
    }

    @Test fun `fields are entered by their kind`() {
        TransferFields.kind(PayerField.PAYER_BIRTHDAY) shouldBe TransferFields.Kind.DATE
        TransferFields.kind(PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE) shouldBe TransferFields.Kind.ACCOUNT_TYPE
        TransferFields.kind(PayerField.PAYER_ID_TYPE) shouldBe TransferFields.Kind.ID_TYPE
        TransferFields.kind(PayerField.RECIPIENT_COUNTRY) shouldBe TransferFields.Kind.COUNTRY
        TransferFields.kind(PayerField.RECIPIENT_ACCOUNT) shouldBe TransferFields.Kind.TEXT
        TransferFields.options(PayerField.PAYER_ID_TYPE).last() shouldBe "IDTP11"
    }

    @Test fun `the birthday is shown as typed and sent as the gateway writes it`() {
        val spec = PayerFieldSpec(PayerField.PAYER_BIRTHDAY, mandatory = true)
        val state =
            PayerFieldsState(
                PayerDataRequest(
                    fieldsByPaymentSystem = mapOf("VISA" to listOf(spec)),
                    prefill =
                        mapOf(
                            PayerField.PAYER_BIRTHDAY to "19901212",
                        ),
                ),
            )

        state.value(PayerField.PAYER_BIRTHDAY) shouldBe "12.12.1990"
        state.onValueChange(PayerField.PAYER_BIRTHDAY, "0307198")
        state.value(PayerField.PAYER_BIRTHDAY) shouldBe "03.07.198"
        state.onValueChange(PayerField.PAYER_BIRTHDAY, "03.07.1985")
        state.submitValues(listOf(spec)) shouldBe mapOf(PayerField.PAYER_BIRTHDAY to "19850703")
    }

    @Test fun `billing then sender then recipient fields are ordered for the form`() {
        val specs =
            listOf(
                PayerField.RECIPIENT_FIRST_NAME,
                PayerField.EMAIL,
                PayerField.PAYER_LAST_NAME,
                PayerField.BILLING_CITY,
                PayerField.PAYER_FIRST_NAME,
            ).map { PayerFieldSpec(it, mandatory = true) }
        val state = PayerFieldsState(PayerDataRequest(fieldsByPaymentSystem = mapOf("VISA" to specs)))

        state.specsFor(listOf("VISA")).map { it.field } shouldBe
            listOf(
                PayerField.EMAIL,
                PayerField.BILLING_CITY,
                PayerField.PAYER_FIRST_NAME,
                PayerField.PAYER_LAST_NAME,
                PayerField.RECIPIENT_FIRST_NAME,
            )
    }

    private companion object {
        const val TODAY = 20261030
    }
}
