package net.payrdr.mobile.payment.sdk.payment.gpay

/**
 * Google Pay environment. [TEST] uses Google's test payment cards; [PRODUCTION] requires a
 * production merchant-id registered in the Google Pay & Wallet console.
 */
public enum class GooglePayEnvironment {
    TEST,
    PRODUCTION,
}
