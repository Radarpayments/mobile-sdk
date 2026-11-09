package net.payrdr.mobile.payment.sdk.payment.ui.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.payer.TransferFields
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneNumbers
import kotlin.time.Clock

/**
 * Checks of the payer fields, mirroring the gateway's payment page: billing text in Latin letters
 * (except the state), alphanumeric postal code; the phone by its country's mobile number lengths.
 */
internal object PayerFieldValidation {
    /** Same character set as the payment page's billing fields. */
    private val LATIN = Regex("^[A-Za-z0-9\\s'\"!#$%&@^`~*+=\\-_.,:;<>|\\uff0c\\u0384\\u00b4\\u2013/?\\\\{}()\\[\\]]+$")
    private val POSTAL_CODE = Regex("^[A-Za-z0-9][A-Za-z0-9 \\-/]*$")

    /** Longest value the gateway accepts per field; input beyond it is cut. */
    fun maxLength(field: PayerField): Int =
        when (field) {
            PayerField.EMAIL -> 100
            PayerField.MOBILE_PHONE -> 24
            PayerField.BILLING_COUNTRY -> 2
            PayerField.BILLING_CITY -> 40
            PayerField.BILLING_POSTAL_CODE -> 10
            PayerField.BILLING_STATE,
            PayerField.BILLING_ADDRESS_LINE1,
            PayerField.BILLING_ADDRESS_LINE2,
            PayerField.BILLING_ADDRESS_LINE3,
            PayerField.RECIPIENT_STATE,
            PayerField.RECIPIENT_ADDRESS_LINE1,
            -> 50
            // Transfer fields: the lengths of the gateway's own page.
            PayerField.PAYER_FIRST_NAME,
            PayerField.PAYER_MIDDLE_NAME,
            PayerField.RECIPIENT_FIRST_NAME,
            PayerField.RECIPIENT_MIDDLE_NAME,
            -> 35
            PayerField.PAYER_LAST_NAME, PayerField.RECIPIENT_LAST_NAME -> 64
            PayerField.PAYER_ACCOUNT, PayerField.RECIPIENT_ACCOUNT -> 32
            PayerField.PAYER_ID_NUMBER, PayerField.RECIPIENT_ID_NUMBER -> 99
            PayerField.RECIPIENT_CITY -> 40
            PayerField.RECIPIENT_POSTAL_CODE -> 10
            PayerField.RECIPIENT_COUNTRY -> 2
            // DD.MM.YYYY on screen.
            PayerField.PAYER_BIRTHDAY, PayerField.RECIPIENT_BIRTHDAY -> 10
            PayerField.PAYER_ACCOUNT_NUMBER_TYPE,
            PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE,
            PayerField.PAYER_ID_TYPE,
            PayerField.RECIPIENT_ID_TYPE,
            -> 6
        }

    fun validate(
        spec: PayerFieldSpec,
        value: String,
        today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    ): FieldError? {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || (spec.field == PayerField.MOBILE_PHONE && trimmed == "+")) {
            return if (spec.mandatory) emptyError(spec.field) else null
        }
        return when (spec.field) {
            PayerField.EMAIL -> CardInputValidation.email(trimmed)
            PayerField.MOBILE_PHONE -> PhoneNumbers.validate(trimmed)
            PayerField.BILLING_COUNTRY,
            PayerField.RECIPIENT_COUNTRY,
            PayerField.PAYER_ACCOUNT_NUMBER_TYPE,
            PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE,
            PayerField.PAYER_ID_TYPE,
            PayerField.RECIPIENT_ID_TYPE,
            -> null
            // Account and document numbers are the bank's own strings: e-mails, phones, IBANs, letters and digits.
            PayerField.PAYER_ACCOUNT,
            PayerField.RECIPIENT_ACCOUNT,
            PayerField.PAYER_ID_NUMBER,
            PayerField.RECIPIENT_ID_NUMBER,
            -> null
            PayerField.PAYER_BIRTHDAY, PayerField.RECIPIENT_BIRTHDAY ->
                if (TransferFields.isValidDate(
                        trimmed,
                        today.year * 10_000 + today.month.number * 100 + today.day,
                    )
                ) {
                    null
                } else {
                    FieldError.DATE_INVALID
                }
            PayerField.BILLING_POSTAL_CODE, PayerField.RECIPIENT_POSTAL_CODE ->
                if (POSTAL_CODE.matches(trimmed)) null else FieldError.POSTAL_CODE_INVALID
            PayerField.BILLING_STATE,
            PayerField.BILLING_CITY,
            PayerField.BILLING_ADDRESS_LINE1,
            PayerField.BILLING_ADDRESS_LINE2,
            PayerField.BILLING_ADDRESS_LINE3,
            PayerField.PAYER_FIRST_NAME,
            PayerField.PAYER_MIDDLE_NAME,
            PayerField.PAYER_LAST_NAME,
            PayerField.RECIPIENT_FIRST_NAME,
            PayerField.RECIPIENT_MIDDLE_NAME,
            PayerField.RECIPIENT_LAST_NAME,
            PayerField.RECIPIENT_STATE,
            PayerField.RECIPIENT_CITY,
            PayerField.RECIPIENT_ADDRESS_LINE1,
            -> if (LATIN.matches(trimmed)) null else FieldError.LATIN_ONLY
        }
    }

    private fun emptyError(field: PayerField): FieldError =
        when (field) {
            PayerField.EMAIL -> FieldError.EMAIL_EMPTY
            PayerField.MOBILE_PHONE -> FieldError.PHONE_EMPTY
            PayerField.BILLING_COUNTRY, PayerField.RECIPIENT_COUNTRY -> FieldError.COUNTRY_EMPTY
            else -> FieldError.FIELD_REQUIRED
        }
}
