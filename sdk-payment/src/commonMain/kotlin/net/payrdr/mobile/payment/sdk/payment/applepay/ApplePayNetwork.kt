package net.payrdr.mobile.payment.sdk.payment.applepay

/**
 * Networks supported by Apple Pay. These map 1:1 to `PKPaymentNetwork` constants on iOS;
 * the mapping lives in `ApplePayNetworkMapping.ios.kt` (iosMain).
 */
public enum class ApplePayNetwork {
    AMEX,
    DISCOVER,
    JCB,
    MASTERCARD,
    VISA,
}
