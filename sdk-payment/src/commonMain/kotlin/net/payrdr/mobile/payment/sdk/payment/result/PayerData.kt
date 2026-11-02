package net.payrdr.mobile.payment.sdk.payment.result

/**
 * Payer detail the gateway may ask for together with the card (`payerDataParamsNeedToBeFilled` in
 * `getSessionStatus.do`). [apiName] is the gateway name of the field.
 *
 * `PAYER_*` and `RECIPIENT_*` fields come with a transfer (AFT): the sender's and the recipient's details. Birthdays
 * are `YYYYMMDD`, account and document types the gateway's codes (`1`…`8`, `100`; `IDTP1`…`IDTP11`).
 */
public enum class PayerField(
    public val apiName: String,
) {
    EMAIL("EMAIL"),
    MOBILE_PHONE("MOBILE_PHONE"),
    BILLING_COUNTRY("BILLING_COUNTRY"),
    BILLING_STATE("BILLING_STATE"),
    BILLING_CITY("BILLING_CITY"),
    BILLING_POSTAL_CODE("BILLING_POSTAL_CODE"),
    BILLING_ADDRESS_LINE1("BILLING_ADDRESS_LINE1"),
    BILLING_ADDRESS_LINE2("BILLING_ADDRESS_LINE2"),
    BILLING_ADDRESS_LINE3("BILLING_ADDRESS_LINE3"),
    PAYER_FIRST_NAME("PAYER_FIRST_NAME"),
    PAYER_MIDDLE_NAME("PAYER_MIDDLE_NAME"),
    PAYER_LAST_NAME("PAYER_LAST_NAME"),
    PAYER_BIRTHDAY("PAYER_BIRTHDAY"),
    PAYER_ACCOUNT("PAYER_ACCOUNT"),
    PAYER_ACCOUNT_NUMBER_TYPE("PAYER_ACCOUNT_NUMBER_TYPE"),
    PAYER_ID_TYPE("PAYER_ID_TYPE"),
    PAYER_ID_NUMBER("PAYER_ID_NUMBER"),
    RECIPIENT_FIRST_NAME("RECIPIENT_FIRST_NAME"),
    RECIPIENT_MIDDLE_NAME("RECIPIENT_MIDDLE_NAME"),
    RECIPIENT_LAST_NAME("RECIPIENT_LAST_NAME"),
    RECIPIENT_BIRTHDAY("RECIPIENT_BIRTHDAY"),
    RECIPIENT_ACCOUNT("RECIPIENT_ACCOUNT"),
    RECIPIENT_ACCOUNT_NUMBER_TYPE("RECIPIENT_ACCOUNT_NUMBER_TYPE"),
    RECIPIENT_COUNTRY("RECIPIENT_COUNTRY"),
    RECIPIENT_STATE("RECIPIENT_STATE"),
    RECIPIENT_CITY("RECIPIENT_CITY"),
    RECIPIENT_ADDRESS_LINE1("RECIPIENT_ADDRESS_LINE1"),
    RECIPIENT_POSTAL_CODE("RECIPIENT_POSTAL_CODE"),
    RECIPIENT_ID_TYPE("RECIPIENT_ID_TYPE"),
    RECIPIENT_ID_NUMBER("RECIPIENT_ID_NUMBER"),
    ;

    public companion object {
        /** Field by its gateway name, or null for names this SDK does not collect. */
        public fun fromApiName(name: String): PayerField? = entries.firstOrNull { it.apiName.equals(name.trim(), ignoreCase = true) }
    }
}

/**
 * One field the gateway asks for.
 *
 * @property mandatory payment is declined without it
 * @property editable false — show the prefilled value read-only
 */
public data class PayerFieldSpec(
    val field: PayerField,
    val mandatory: Boolean,
    val editable: Boolean = true,
)

/**
 * Payer fields the gateway asks to collect, per payment system of the card, with prefill values.
 *
 * The gateway lists fields per payment system (`VISA`, `MASTERCARD`, …) and drops the ones the
 * merchant already passed when registering the order, so the set changes with the card being paid.
 *
 * @property fieldsByPaymentSystem upper-case payment system → fields, in the gateway's order
 * @property prefill values known from the order (`customerDetails`, `orderPayerData`, `billingPayerData`)
 * @property phoneRegion country (ISO 3166-1 alpha-2) the gateway suggests for the payer's phone, e.g. `GB`
 */
public data class PayerDataRequest(
    val fieldsByPaymentSystem: Map<String, List<PayerFieldSpec>> = emptyMap(),
    val prefill: Map<PayerField, String> = emptyMap(),
    val phoneRegion: String? = null,
) {
    /** True when no payment system asks for anything. */
    public val isEmpty: Boolean get() = fieldsByPaymentSystem.values.all { it.isEmpty() }

    /**
     * Fields to collect for a card of [paymentSystem] (gateway name, e.g. `VISA`, or `MASTERCARD`);
     * empty when the payment system is unknown or asks for nothing.
     */
    public fun fieldsFor(paymentSystem: String?): List<PayerFieldSpec> {
        val key = paymentSystem?.trim()?.uppercase()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        return fieldsByPaymentSystem[key].orEmpty()
    }
}
