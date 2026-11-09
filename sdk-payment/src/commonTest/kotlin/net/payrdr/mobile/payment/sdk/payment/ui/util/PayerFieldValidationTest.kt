package net.payrdr.mobile.payment.sdk.payment.ui.util

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import kotlin.test.Test

class PayerFieldValidationTest {
    private fun check(
        field: PayerField,
        value: String,
        mandatory: Boolean = true,
    ) = PayerFieldValidation.validate(PayerFieldSpec(field, mandatory), value, today = LocalDate(2026, 11, 5))

    @Test
    fun `empty is an error only when mandatory`() {
        check(PayerField.EMAIL, " ") shouldBe FieldError.EMAIL_EMPTY
        check(PayerField.MOBILE_PHONE, "+") shouldBe FieldError.PHONE_EMPTY
        check(PayerField.BILLING_COUNTRY, "") shouldBe FieldError.COUNTRY_EMPTY
        check(PayerField.BILLING_CITY, "") shouldBe FieldError.FIELD_REQUIRED
        check(PayerField.BILLING_CITY, "", mandatory = false) shouldBe null
        check(PayerField.EMAIL, "", mandatory = false) shouldBe null
    }

    @Test
    fun `optional fields are still checked when filled`() {
        check(PayerField.EMAIL, "name@example", mandatory = false) shouldBe FieldError.EMAIL_INVALID
        check(PayerField.BILLING_CITY, "София", mandatory = false) shouldBe FieldError.LATIN_ONLY
    }

    @Test
    fun `phone is checked against its country's mobile number lengths`() {
        check(PayerField.MOBILE_PHONE, "+447700900123") shouldBe null
        check(PayerField.MOBILE_PHONE, "+35796123456") shouldBe null
        check(PayerField.MOBILE_PHONE, "7700900123") shouldBe FieldError.PHONE_NO_CODE
        check(PayerField.MOBILE_PHONE, "+44770090") shouldBe FieldError.PHONE_SHORT
        check(PayerField.MOBILE_PHONE, "+4477009001234") shouldBe FieldError.PHONE_LONG
    }

    @Test
    fun `billing text is Latin like on the payment page`() {
        check(PayerField.BILLING_ADDRESS_LINE1, "221B Baker Street, Flat 2/3") shouldBe null
        check(PayerField.BILLING_ADDRESS_LINE2, "Straße 5") shouldBe FieldError.LATIN_ONLY
    }

    @Test
    fun `the state is Latin too as the gateway takes it`() {
        check(PayerField.BILLING_STATE, "Ile-de-France") shouldBe null
        check(PayerField.BILLING_STATE, "Île-de-France") shouldBe FieldError.LATIN_ONLY
        check(PayerField.BILLING_STATE, "Софийска област") shouldBe FieldError.LATIN_ONLY
    }

    @Test
    fun `postal code is alphanumeric`() {
        check(PayerField.BILLING_POSTAL_CODE, "NW1 6XE") shouldBe null
        check(PayerField.BILLING_POSTAL_CODE, "10115") shouldBe null
        check(PayerField.BILLING_POSTAL_CODE, "NW1#6XE") shouldBe FieldError.POSTAL_CODE_INVALID
    }

    @Test
    fun `a birthday is a real past day`() {
        check(PayerField.PAYER_BIRTHDAY, "29.02.2000") shouldBe null
        check(PayerField.RECIPIENT_BIRTHDAY, "05.11.2026") shouldBe null
        check(PayerField.PAYER_BIRTHDAY, "29.02.2001") shouldBe FieldError.DATE_INVALID
        check(PayerField.PAYER_BIRTHDAY, "06.11.2026") shouldBe FieldError.DATE_INVALID
        check(PayerField.RECIPIENT_BIRTHDAY, "1990-03-15") shouldBe FieldError.DATE_INVALID
    }

    @Test
    fun `account and document numbers are the bank's own strings and are not checked`() {
        check(PayerField.PAYER_ACCOUNT, "GB33 BUKB 2020 1555 5555 55") shouldBe null
        check(PayerField.RECIPIENT_ACCOUNT, "name@example.com") shouldBe null
        check(PayerField.PAYER_ID_NUMBER, "№ 12-34") shouldBe null
        check(PayerField.RECIPIENT_ID_TYPE, "PASSPORT") shouldBe null
    }

    @Test
    fun `the recipient's names and address follow the billing rules`() {
        check(PayerField.RECIPIENT_FIRST_NAME, "Rita") shouldBe null
        check(PayerField.RECIPIENT_LAST_NAME, "Иванова") shouldBe FieldError.LATIN_ONLY
        check(PayerField.RECIPIENT_POSTAL_CODE, "-1000") shouldBe FieldError.POSTAL_CODE_INVALID
        check(PayerField.RECIPIENT_COUNTRY, "") shouldBe FieldError.COUNTRY_EMPTY
    }

    @Test
    fun `formatted fields fit exactly what they hold`() {
        PayerFieldValidation.maxLength(PayerField.PAYER_BIRTHDAY) shouldBe "DD.MM.YYYY".length
        PayerFieldValidation.maxLength(PayerField.RECIPIENT_BIRTHDAY) shouldBe "DD.MM.YYYY".length
        PayerFieldValidation.maxLength(PayerField.BILLING_COUNTRY) shouldBe 2
        PayerFieldValidation.maxLength(PayerField.RECIPIENT_COUNTRY) shouldBe 2
    }
}
