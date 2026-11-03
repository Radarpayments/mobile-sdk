package net.payrdr.mobile.payment.sdk.payment.api.dto.response

/**
 * The merchant's checkout settings returned by `getPaymentSettings.do`: a flat set of named values.
 *
 * @property values every setting the gateway returned, by name (e.g. `googlePay.gateway`)
 */
internal data class PaymentSettings(
    val values: Map<String, String>,
) {
    /** `googlePay.environment`: `TEST` or `PRODUCTION`. */
    val googlePayEnvironment: String? get() = values["googlePay.environment"]

    /** `googlePay.merchantId`: the merchant's id in the Google Pay & Wallet console. */
    val googlePayMerchantId: String? get() = values["googlePay.merchantId"]

    /** `googlePay.gateway`: the gateway name Google Pay encrypts the token for. */
    val googlePayGateway: String? get() = values["googlePay.gateway"]

    /** `ecom2.hidden.payments`: payment methods the merchant hid (`GOOGLE_PAY`, `APPLE_PAY`, …). */
    val hiddenPayments: Set<String>
        get() =
            values["ecom2.hidden.payments"]
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                .orEmpty()
}
