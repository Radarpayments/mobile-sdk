package net.payrdr.mobile.payment.sdk.payment.applepay

/**
 * Merchant capability flags passed into `PKPaymentRequest.merchantCapabilities`. At least one
 * of [CREDIT] / [DEBIT] must be present; [THREE_D_SECURE] should always be present for modern
 * card acceptance.
 */
public enum class ApplePayMerchantCapability {
    THREE_D_SECURE,
    EMV,
    CREDIT,
    DEBIT,
}
