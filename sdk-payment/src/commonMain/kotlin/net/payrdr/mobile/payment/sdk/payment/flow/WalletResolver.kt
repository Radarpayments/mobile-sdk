package net.payrdr.mobile.payment.sdk.payment.flow

import net.payrdr.mobile.payment.sdk.payment.api.dto.response.PaymentSettings
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment

/**
 * The wallets an order can be paid with, and with what: the merchant's config over the gateway's settings and
 * the order. A wallet shows only when the gateway has enabled it for the merchant and not hidden it — as on the
 * gateway's payment page — and when every value it needs is known; nothing is guessed.
 */
internal object WalletResolver {
    /** `merchantOptions` entries that enable the wallets; `ecom2.hidden.payments` names that hide them. */
    const val GOOGLE_PAY_OPTION = "GOOGLEPAY"
    const val APPLE_PAY_OPTION = "APPLEPAY"
    private const val GOOGLE_PAY_HIDDEN = "GOOGLE_PAY"
    private const val APPLE_PAY_HIDDEN = "APPLE_PAY"

    /** [config] completed with every value Google Pay needs, or null when this order cannot be paid with it. */
    fun googlePay(
        config: GooglePayConfig,
        session: SessionStatusResponse,
        settings: PaymentSettings?,
    ): GooglePayConfig? {
        if (GOOGLE_PAY_OPTION !in session.merchantOptions) return null
        if (settings != null && GOOGLE_PAY_HIDDEN in settings.hiddenPayments) return null
        val environment =
            config.environment
                ?: settings?.googlePayEnvironment?.let { name ->
                    GooglePayEnvironment.entries.find { it.name.equals(name, ignoreCase = true) }
                }
        return config
            .copy(
                merchantId = config.merchantId.given() ?: settings?.googlePayMerchantId,
                merchantFullName = config.merchantFullName.given() ?: session.merchantInfo.merchantFullName,
                gateway = config.gateway.given() ?: settings?.googlePayGateway,
                gatewayMerchantId = config.gatewayMerchantId.given() ?: session.merchantInfo.merchantLogin,
                environment = environment,
                countryCode = config.countryCode.given() ?: session.merchantInfo.merchantCountry?.countryCodeAlpha2,
            ).takeIf {
                listOf(it.merchantId, it.gateway, it.gatewayMerchantId).none { value -> value.isNullOrBlank() } &&
                    it.environment != null &&
                    it.allowedCardNetworks.isNotEmpty() &&
                    it.allowedAuthMethods.isNotEmpty() &&
                    isCountry(it.countryCode) &&
                    isCurrency(session.currencyAlphaCode)
            }
    }

    /** [config] with its country, or null when this order cannot be paid with Apple Pay (the currency is the order's). */
    fun applePay(
        config: ApplePayConfig,
        session: SessionStatusResponse,
        settings: PaymentSettings?,
    ): ApplePayConfig? {
        if (APPLE_PAY_OPTION !in session.merchantOptions) return null
        if (settings != null && APPLE_PAY_HIDDEN in settings.hiddenPayments) return null
        return config
            .copy(
                countryCode = config.countryCode.given() ?: session.merchantInfo.merchantCountry?.countryCodeAlpha2,
            ).takeIf {
                it.supportedNetworks.isNotEmpty() &&
                    it.merchantCapabilities.isNotEmpty() &&
                    isCountry(it.countryCode) &&
                    isCurrency(session.currencyAlphaCode)
            }
    }

    /** A value the merchant actually set: a blank one leaves it to the gateway and the order. */
    private fun String?.given(): String? = this?.takeIf { it.isNotBlank() }

    // The wallets reject anything else: ISO 3166-1 alpha-2 and ISO 4217 alpha-3.
    private fun isCountry(code: String?): Boolean = code?.length == 2

    private fun isCurrency(code: String?): Boolean = code?.length == 3
}
