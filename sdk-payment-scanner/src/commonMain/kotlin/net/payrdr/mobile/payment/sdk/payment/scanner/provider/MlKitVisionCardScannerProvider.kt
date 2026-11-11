package net.payrdr.mobile.payment.sdk.payment.scanner.provider

import net.payrdr.mobile.payment.sdk.payment.logger.LogTransport
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider

/**
 * Default [CardScannerProvider] shipping ML Kit (Android) + Vision (iOS) camera OCR.
 * Instantiated by merchants who want camera scanning and passed to `SdkPaymentConfig.cardScannerProvider`.
 *
 * @param logTransport optional sink for scanner events (`event=...`, tag `"CardScanner"`), usually the one of
 *   `SdkPaymentConfig.logTransport`, so scanner events flow through the same sink.
 */
public expect class MlKitVisionCardScannerProvider(
    logTransport: LogTransport? = null,
) : CardScannerProvider {
    internal val logger: Logger?
}
