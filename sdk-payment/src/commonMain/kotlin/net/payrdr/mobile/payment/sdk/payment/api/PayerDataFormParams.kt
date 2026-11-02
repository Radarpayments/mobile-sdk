package net.payrdr.mobile.payment.sdk.payment.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import net.payrdr.mobile.payment.sdk.payment.result.PayerField

/**
 * Payer values → `processform.do` / `processBindingForm.do` form parameters, the way the gateway's
 * payment page sends them: `email`, `orderPayerData={"mobilePhone":…}` and `billingPayerData={…}`
 * as JSON strings. Blank values are dropped.
 */
internal object PayerDataFormParams {
    private val BILLING_KEYS =
        linkedMapOf(
            PayerField.BILLING_COUNTRY to "billingCountry",
            PayerField.BILLING_STATE to "billingState",
            PayerField.BILLING_CITY to "billingCity",
            PayerField.BILLING_POSTAL_CODE to "billingPostalCode",
            PayerField.BILLING_ADDRESS_LINE1 to "billingAddressLine1",
            PayerField.BILLING_ADDRESS_LINE2 to "billingAddressLine2",
            PayerField.BILLING_ADDRESS_LINE3 to "billingAddressLine3",
        )

    /** Keys of the sender fields of a transfer in `billingPayerData`. */
    private val SENDER_KEYS =
        linkedMapOf(
            PayerField.PAYER_FIRST_NAME to "payerFirstName",
            PayerField.PAYER_MIDDLE_NAME to "payerMiddleName",
            PayerField.PAYER_LAST_NAME to "payerLastName",
            PayerField.PAYER_BIRTHDAY to "payerBirthday",
            PayerField.PAYER_ACCOUNT to "payerAccount",
            PayerField.PAYER_ACCOUNT_NUMBER_TYPE to "payerAccountNumberType",
            PayerField.PAYER_ID_TYPE to "payerIdType",
            PayerField.PAYER_ID_NUMBER to "payerIdNumber",
        )

    /** Keys of the recipient fields of a transfer in `billingRecipientData`. */
    private val RECIPIENT_KEYS =
        linkedMapOf(
            PayerField.RECIPIENT_FIRST_NAME to "recipientFirstName",
            PayerField.RECIPIENT_MIDDLE_NAME to "recipientMiddleName",
            PayerField.RECIPIENT_LAST_NAME to "recipientLastName",
            PayerField.RECIPIENT_BIRTHDAY to "recipientBirthday",
            PayerField.RECIPIENT_ACCOUNT to "recipientAccount",
            PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE to "recipientAccountNumberType",
            PayerField.RECIPIENT_COUNTRY to "recipientCountry",
            PayerField.RECIPIENT_STATE to "recipientState",
            PayerField.RECIPIENT_CITY to "recipientCity",
            PayerField.RECIPIENT_ADDRESS_LINE1 to "recipientAddressLine1",
            PayerField.RECIPIENT_POSTAL_CODE to "recipientPostalCode",
            PayerField.RECIPIENT_ID_TYPE to "recipientIdType",
            PayerField.RECIPIENT_ID_NUMBER to "recipientIdNumber",
        )

    /**
     * Payer values → the JSON fields of a transfer (`p2p/perform.do`), the way the gateway's page sends them: `email`,
     * `orderPayerData.mobilePhone`, `billingPayerData` (the billing address and the sender) and `billingRecipientData`.
     */
    fun transfer(payerData: Map<PayerField, String>): JsonObject {
        val values = payerData.mapValues { it.value.trim() }.filterValues { it.isNotEmpty() }

        fun group(keys: Map<PayerField, String>) =
            keys.mapNotNull { (field, key) -> values[field]?.let { key to JsonPrimitive(it) } }.toMap()
        return buildJsonObject {
            values[PayerField.EMAIL]?.let { put("email", JsonPrimitive(it)) }
            values[PayerField.MOBILE_PHONE]?.let {
                put(
                    "orderPayerData",
                    JsonObject(mapOf("mobilePhone" to JsonPrimitive(normalizePhone(it)))),
                )
            }
            group(BILLING_KEYS + SENDER_KEYS).takeIf { it.isNotEmpty() }?.let { put("billingPayerData", JsonObject(it)) }
            group(RECIPIENT_KEYS).takeIf { it.isNotEmpty() }?.let { put("billingRecipientData", JsonObject(it)) }
        }
    }

    fun build(payerData: Map<PayerField, String>): List<Pair<String, String>> {
        val values = payerData.mapValues { it.value.trim() }.filterValues { it.isNotEmpty() }
        return buildList {
            values[PayerField.EMAIL]?.let { add("email" to it) }
            values[PayerField.MOBILE_PHONE]?.let { phone ->
                add("orderPayerData" to json(mapOf("mobilePhone" to normalizePhone(phone))))
            }
            val billing = BILLING_KEYS.mapNotNull { (field, key) -> values[field]?.let { key to it } }.toMap()
            if (billing.isNotEmpty()) add("billingPayerData" to json(billing))
        }
    }

    /** `+44 (20) 7946-0958` → `+442079460958`. */
    fun normalizePhone(value: String): String {
        val digits = value.filter(Char::isDigit)
        return if (value.trim().startsWith("+")) "+$digits" else digits
    }

    private fun json(map: Map<String, String>): String {
        val obj = JsonObject(map.mapValues { JsonPrimitive(it.value) })
        return Json.encodeToString(JsonObject.serializer(), obj)
    }
}
