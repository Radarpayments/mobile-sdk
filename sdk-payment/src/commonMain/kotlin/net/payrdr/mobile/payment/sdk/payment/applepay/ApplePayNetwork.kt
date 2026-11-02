package net.payrdr.mobile.payment.sdk.payment.applepay

/**
 * Networks supported by Apple Pay. These map 1:1 to `PKPaymentNetwork` constants on iOS;
 * the mapping lives in `ApplePayNetworkMapping.ios.kt` (iosMain).
 *
 * Values not supported by the current iOS deployment target (15.0) are gracefully filtered
 * out by the platform actual.
 */
public enum class ApplePayNetwork {
    AMEX,
    DISCOVER,
    JCB,
    MASTERCARD,
    VISA,
}
