package net.payrdr.mobile.payment.sdk.payment.config

import net.payrdr.mobile.payment.sdk.payment.logger.LogTransport
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

/**
 * Top-level configuration passed to [net.payrdr.mobile.payment.sdk.payment.SdkPayment.create].
 *
 * @property baseUrl payment gateway base URL, e.g. `https://dev.bpcbt.com/payment`
 * @property keyProviderUrl URL that returns the active RSA public key set; defaults to `{baseUrl}/se/keys.do`
 * @property sslPinning opt-in certificate pinning config; null = system trust store, no pinning
 * @property logTransport sink for internal logging; null = [net.payrdr.mobile.payment.sdk.payment.logger.NoopLogTransport]
 * @property locale language of the checkout screens, a tag such as `"de"` or `"ar"` (Arabic is laid out right to
 *   left); see [net.payrdr.mobile.payment.sdk.payment.ui.SupportedPaymentLanguages]. null = the system language
 * @property theme UI theme to use for the turnkey checkout screens; default = [PaymentTheme.System]
 * @property googlePay opt-in Google Pay configuration; null = Google Pay button hidden
 * @property applePay opt-in Apple Pay configuration; null = Apple Pay button hidden
 * @property cardScannerProvider opt-in card scanner provider; null = no camera scan icon on the card form
 * @property showResultScreen when true, the checkout shows its own success / declined screen with the
 *   receipt and delivers the result only after the user taps Done; false (default) delivers the result
 *   to the merchant immediately
 */
@OptIn(ExperimentalObjCName::class)
public data class SdkPaymentConfig(
    // Swift calls `SdkPaymentConfig(baseUrl:…)` with defaults (bundled Swift initializer); this full constructor gets a
    // label of its own so that initializer can call it.
    @ObjCName(swiftName = "__baseUrl")
    val baseUrl: String,
    val keyProviderUrl: String? = null,
    val sslPinning: SslPinningConfig? = null,
    val logTransport: LogTransport? = null,
    val locale: String? = null,
    val theme: PaymentTheme = PaymentTheme.System,
    val googlePay: GooglePayConfig? = null,
    val applePay: ApplePayConfig? = null,
    /**
     * Opt-in card scanner provider; null = no camera scan icon on the card form.
     * Merchants who want scanning add an `sdk-payment-scanner` dependency and pass
     * `MlKitVisionCardScannerProvider()` here.
     */
    val cardScannerProvider: CardScannerProvider? = null,
    val showResultScreen: Boolean = false,
) {
    /** Resolves [keyProviderUrl] to its effective value (default if null). */
    public fun effectiveKeyProviderUrl(): String = keyProviderUrl ?: "$baseUrl/se/keys.do"
}
