package net.payrdr.mobile.payment.sdk.payment.flow

import net.payrdr.mobile.payment.sdk.payment.api.dto.response.BillingPayerData
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.BillingRecipientData
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.BindingItem
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.payer.TransferFields

/** Gateway session → [PayerDataRequest], following the gateway's own payment page. */
internal object PayerDataMapper {
    fun request(session: SessionStatusResponse): PayerDataRequest {
        val fields =
            session.payerDataParamsNeedToBeFilled
                .orEmpty()
                .mapKeys { (system, _) -> system.trim().uppercase() }
                .mapValues { (_, params) ->
                    // Names this SDK does not collect are skipped.
                    params
                        .mapNotNull { p -> PayerField.fromApiName(p.name)?.let { PayerFieldSpec(it, p.mandatory, p.editable) } }
                        .distinctBy { it.field }
                }.filterValues { it.isNotEmpty() }
        val suggested = session.customerDetails?.suggestedCountryCodes.orEmpty()
        val prefill =
            buildMap {
                putIfPresent(PayerField.EMAIL, session.customerDetails?.email ?: session.orderPayerData?.email)
                putIfPresent(PayerField.MOBILE_PHONE, session.orderPayerData?.mobilePhone ?: session.customerDetails?.phone)
                putAll(billing(session.billingPayerData))
                putAll(sender(session.billingPayerData))
                putAll(recipient(session.billingRecipientData))
                if (PayerField.BILLING_COUNTRY !in this) {
                    putIfPresent(PayerField.BILLING_COUNTRY, suggested.firstNotNullOfOrNull { it.alpha2?.takeIf(Countries::isKnown) })
                }
            }
        return PayerDataRequest(
            fieldsByPaymentSystem = fields,
            prefill = prefill,
            phoneRegion =
                suggested.firstNotNullOfOrNull {
                    it.alpha2
                        ?.trim()
                        ?.uppercase()
                        ?.takeIf(Countries::isKnown)
                },
        )
    }

    /** Payer details the gateway remembered with a binding. */
    fun bindingPrefill(item: BindingItem): Map<PayerField, String> =
        buildMap {
            putIfPresent(PayerField.EMAIL, item.payerEmail)
            putIfPresent(PayerField.MOBILE_PHONE, item.payerPhone)
            putAll(billing(item.bindingBillingData))
        }

    private fun billing(data: BillingPayerData?): Map<PayerField, String> =
        buildMap {
            if (data == null) return@buildMap
            putIfPresent(PayerField.BILLING_COUNTRY, data.billingCountry?.uppercase()?.takeIf(Countries::isKnown))
            putIfPresent(PayerField.BILLING_STATE, data.billingState)
            putIfPresent(PayerField.BILLING_CITY, data.billingCity)
            putIfPresent(PayerField.BILLING_POSTAL_CODE, data.billingPostalCode)
            putIfPresent(PayerField.BILLING_ADDRESS_LINE1, data.billingAddressLine1)
            putIfPresent(PayerField.BILLING_ADDRESS_LINE2, data.billingAddressLine2)
            putIfPresent(PayerField.BILLING_ADDRESS_LINE3, data.billingAddressLine3)
        }

    /** The sender of a transfer, as the merchant registered it. */
    private fun sender(data: BillingPayerData?): Map<PayerField, String> =
        buildMap {
            if (data == null) return@buildMap
            putIfPresent(PayerField.PAYER_FIRST_NAME, data.payerFirstName)
            putIfPresent(PayerField.PAYER_MIDDLE_NAME, data.payerMiddleName)
            putIfPresent(PayerField.PAYER_LAST_NAME, data.payerLastName)
            putIfPresent(PayerField.PAYER_BIRTHDAY, data.payerBirthday)
            putIfPresent(PayerField.PAYER_ACCOUNT, data.payerAccount)
            putIfPresent(PayerField.PAYER_ACCOUNT_NUMBER_TYPE, data.payerAccountNumberType?.let(TransferFields::accountTypeCode))
            putIfPresent(PayerField.PAYER_ID_TYPE, data.payerIdType)
            putIfPresent(PayerField.PAYER_ID_NUMBER, data.payerIdNumber)
        }

    /** The recipient of a transfer, as the merchant registered it. */
    private fun recipient(data: BillingRecipientData?): Map<PayerField, String> =
        buildMap {
            if (data == null) return@buildMap
            putIfPresent(PayerField.RECIPIENT_FIRST_NAME, data.recipientFirstName)
            putIfPresent(PayerField.RECIPIENT_MIDDLE_NAME, data.recipientMiddleName)
            putIfPresent(PayerField.RECIPIENT_LAST_NAME, data.recipientLastName)
            putIfPresent(PayerField.RECIPIENT_BIRTHDAY, data.recipientBirthday)
            putIfPresent(PayerField.RECIPIENT_ACCOUNT, data.recipientAccount)
            putIfPresent(PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE, data.recipientAccountNumberType?.let(TransferFields::accountTypeCode))
            putIfPresent(PayerField.RECIPIENT_COUNTRY, data.recipientCountry?.uppercase()?.takeIf(Countries::isKnown))
            putIfPresent(PayerField.RECIPIENT_STATE, data.recipientState)
            putIfPresent(PayerField.RECIPIENT_CITY, data.recipientCity)
            putIfPresent(PayerField.RECIPIENT_ADDRESS_LINE1, data.recipientAddressLine1)
            putIfPresent(PayerField.RECIPIENT_POSTAL_CODE, data.recipientPostalCode)
            putIfPresent(PayerField.RECIPIENT_ID_TYPE, data.recipientIdType)
            putIfPresent(PayerField.RECIPIENT_ID_NUMBER, data.recipientIdNumber)
        }

    private fun MutableMap<PayerField, String>.putIfPresent(
        field: PayerField,
        value: String?,
    ) {
        value?.trim()?.takeIf { it.isNotEmpty() }?.let { put(field, it) }
    }
}
