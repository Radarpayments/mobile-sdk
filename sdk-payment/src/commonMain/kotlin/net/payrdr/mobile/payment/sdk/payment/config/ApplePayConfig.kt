package net.payrdr.mobile.payment.sdk.payment.config

import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayMerchantCapability
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayNetwork
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Apple Pay opt-in configuration. Present → the checkout offers Apple Pay (on iOS; Android renders nothing)
 * when the gateway has enabled it for the merchant.
 *
 * Only [merchantId] is required: the country (the merchant's) comes from the order and [countryCode] overrides it.
 * The currency is always the order's: the amount is charged in it.
 *
 * @property merchantId Apple Pay merchant identifier (the `merchant.` prefix, e.g. `merchant.net.payrdr.app`).
 * @property supportedNetworks networks accepted by the merchant.
 * @property merchantCapabilities `PKPaymentRequest.merchantCapabilities` flags.
 * @property countryCode ISO 3166-1 alpha-2 country.
 */
@OptIn(ExperimentalObjCName::class)
public data class ApplePayConfig(
    // Swift calls `ApplePayConfig(merchantId:…)` with defaults (bundled Swift initializer); see SdkPaymentConfig.
    @param:ObjCName(swiftName = "__merchantId")
    val merchantId: String,
    val supportedNetworks: Set<ApplePayNetwork> =
        setOf(
            ApplePayNetwork.VISA,
            ApplePayNetwork.MASTERCARD,
        ),
    val merchantCapabilities: Set<ApplePayMerchantCapability> =
        setOf(
            ApplePayMerchantCapability.THREE_D_SECURE,
            ApplePayMerchantCapability.CREDIT,
            ApplePayMerchantCapability.DEBIT,
        ),
    val countryCode: String? = null,
)
