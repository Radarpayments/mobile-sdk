package net.payrdr.mobile.payment.sdk.payment.gpay

/**
 * Runtime check that `com.google.android.gms:play-services-wallet` is on the classpath.
 *
 * Declared `compileOnly` in `sdk-payment/build.gradle.kts`, so merchants who don't include
 * `play-services-wallet` as an `implementation` dependency in their app module will not
 * crash — [AndroidGooglePayTokenProvider] falls back to `NotAvailable`.
 *
 * Button availability (PayButton class) is checked independently in
 * `GooglePayButton.android.kt` — older `play-services-wallet` artifacts ship the core
 * PaymentsClient without PayButton, and we want GPay to still work there (just with a
 * fallback button).
 */
internal object GooglePayAvailability {
    val isAvailable: Boolean by lazy {
        try {
            Class.forName("com.google.android.gms.wallet.Wallet")
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: NoClassDefFoundError) {
            false
        }
    }
}
