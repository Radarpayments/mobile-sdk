package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import kotlinx.serialization.Serializable

/** Recipient of a transfer (`billingRecipientData` of `getSessionStatus.do`); [recipientBirthday] is `YYYYMMDD`. */
@Serializable
public data class BillingRecipientData(
    val recipientAccount: String? = null,
    val recipientAccountNumberType: String? = null,
    val recipientFirstName: String? = null,
    val recipientMiddleName: String? = null,
    val recipientLastName: String? = null,
    val recipientBirthday: String? = null,
    val recipientCountry: String? = null,
    val recipientState: String? = null,
    val recipientCity: String? = null,
    val recipientAddressLine1: String? = null,
    val recipientPostalCode: String? = null,
    val recipientIdType: String? = null,
    val recipientIdNumber: String? = null,
)
