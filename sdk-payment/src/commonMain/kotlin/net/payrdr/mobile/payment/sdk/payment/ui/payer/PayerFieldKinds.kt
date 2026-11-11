package net.payrdr.mobile.payment.sdk.payment.ui.payer

import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextFieldContentType

/** One of the billing address lines. */
internal val PayerField.isAddressLine: Boolean
    get() =
        this == PayerField.BILLING_ADDRESS_LINE1 ||
            this == PayerField.BILLING_ADDRESS_LINE2 ||
            this == PayerField.BILLING_ADDRESS_LINE3

/** How the platform fills and types [this] field. */
internal val PayerField.contentType: TextFieldContentType
    get() =
        when (this) {
            PayerField.EMAIL -> TextFieldContentType.EMAIL
            PayerField.MOBILE_PHONE -> TextFieldContentType.PHONE
            PayerField.BILLING_CITY, PayerField.RECIPIENT_CITY -> TextFieldContentType.CITY
            PayerField.BILLING_STATE, PayerField.RECIPIENT_STATE -> TextFieldContentType.STATE
            PayerField.BILLING_POSTAL_CODE, PayerField.RECIPIENT_POSTAL_CODE -> TextFieldContentType.POSTAL_CODE
            PayerField.BILLING_ADDRESS_LINE1,
            PayerField.BILLING_ADDRESS_LINE2,
            PayerField.BILLING_ADDRESS_LINE3,
            PayerField.RECIPIENT_ADDRESS_LINE1,
            -> TextFieldContentType.ADDRESS_LINE
            else -> TextFieldContentType.NONE
        }
