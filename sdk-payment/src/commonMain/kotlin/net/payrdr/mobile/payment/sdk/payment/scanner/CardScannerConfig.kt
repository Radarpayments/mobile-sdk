package net.payrdr.mobile.payment.sdk.payment.scanner

/**
 * Runtime tuning for the scanner screen.
 *
 * @property extractCardholder if true, the parser attempts to extract the cardholder line. Accuracy is
 *   lower than PAN/expiry (~60-70%); default false.
 * @property timeoutSeconds how long the screen will keep scanning before returning `Failed(Timeout)`. 30s default.
 * @property enableTorchButton whether the torch icon is shown in the top bar.
 */
public data class CardScannerConfig(
    val extractCardholder: Boolean = false,
    val timeoutSeconds: Int = 30,
    val enableTorchButton: Boolean = true,
)
