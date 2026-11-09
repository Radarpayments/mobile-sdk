package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/**
 * Billing address of the payer, as the gateway names it in `getSessionStatus.do` and `processform.do`; a transfer adds
 * the sender's account, name, birthday (`YYYYMMDD`) and identity document.
 */
@Serializable
internal data class BillingPayerData(
    val billingCountry: String? = null,
    val billingState: String? = null,
    val billingCity: String? = null,
    val billingPostalCode: String? = null,
    val billingAddressLine1: String? = null,
    val billingAddressLine2: String? = null,
    val billingAddressLine3: String? = null,
    val payerAccount: String? = null,
    val payerAccountNumberType: String? = null,
    val payerFirstName: String? = null,
    val payerMiddleName: String? = null,
    val payerLastName: String? = null,
    val payerBirthday: String? = null,
    val payerIdType: String? = null,
    val payerIdNumber: String? = null,
)
