package net.payrdr.mobile.payment.sdk.payment.config

import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayAuthMethod
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayCardNetwork
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment

/**
 * Google Pay opt-in configuration. Present → the checkout offers Google Pay (on Android; iOS renders nothing)
 * when the gateway has enabled it for the merchant.
 *
 * `GooglePayConfig()` is enough: the SDK takes the Google Pay merchant id, gateway and environment from the
 * gateway's settings for the merchant, the gateway merchant id (the merchant's login), the merchant's name and
 * country from the order. Every value set here overrides what the SDK would take. The currency is always the order's:
 * the amount is charged in it.
 *
 * @property merchantId Google Pay merchant identifier registered in the Wallet console.
 * @property merchantFullName display name shown in the Google Pay sheet header.
 * @property gateway payment gateway name Google Pay encrypts the token for, e.g. `"bpcpay"`.
 * @property gatewayMerchantId merchant identifier understood by [gateway].
 * @property environment [GooglePayEnvironment.TEST] for sandbox, [GooglePayEnvironment.PRODUCTION] for live.
 * @property allowedCardNetworks networks accepted by the merchant.
 * @property allowedAuthMethods authorization methods accepted by the merchant.
 * @property countryCode ISO 3166-1 alpha-2 country code.
 * @property billingAddressRequired whether GPay should prompt for billing address.
 */
public data class GooglePayConfig(
    val merchantId: String? = null,
    val merchantFullName: String? = null,
    val gateway: String? = null,
    val gatewayMerchantId: String? = null,
    val environment: GooglePayEnvironment? = null,
    val allowedCardNetworks: Set<GooglePayCardNetwork> =
        setOf(
            GooglePayCardNetwork.VISA,
            GooglePayCardNetwork.MASTERCARD,
        ),
    val allowedAuthMethods: Set<GooglePayAuthMethod> =
        setOf(
            GooglePayAuthMethod.PAN_ONLY,
            GooglePayAuthMethod.CRYPTOGRAM_3DS,
        ),
    val countryCode: String? = null,
    val billingAddressRequired: Boolean = false,
)
