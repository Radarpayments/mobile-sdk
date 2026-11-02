package net.payrdr.mobile.payment.sdk.payment.ui.util

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import kotlin.test.Test

class PayerFieldValidationTest {
    private fun check(
        field: PayerField,
        value: String,
        mandatory: Boolean = true,
    ) = PayerFieldValidation.validate(PayerFieldSpec(field, mandatory), value)

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
}
