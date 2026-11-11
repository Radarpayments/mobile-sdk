package net.payrdr.mobile.payment.sdk.payment.scanner.provider

import net.payrdr.mobile.payment.sdk.payment.logger.LogTransport
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.presentation.NativeCardScanner
import net.payrdr.mobile.payment.sdk.payment.presentation.NativeScannerSession
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.ScannerCamera
import net.payrdr.mobile.payment.sdk.payment.scanner.presentation.VisionScannerSession

/**
 * The card scanner of the checkout: Vision reads the card from the camera. Pass it to
 * `SdkPaymentConfig.cardScannerProvider`.
 *
 * An app creates it with `MlKitVisionCardScannerProvider(logTransport:)` from the `RadarpaymentsSDKScanner` module,
 * which supplies the [camera]; the payment framework itself has no camera code.
 *
 * @param logTransport optional sink for scanner events (`event=...`, tag `"CardScanner"`), usually the one of
 *   `SdkPaymentConfig.logTransport`, so scanner events flow through the same sink.
 */
public class MlKitVisionCardScannerProvider(
    private val camera: ScannerCamera,
    logTransport: LogTransport? = null,
) : CardScannerProvider,
    NativeCardScanner {
    private val logger: Logger? = logTransport?.let { Logger(transports = listOf(it)) }

    override fun openScanner(
        config: CardScannerConfig,
        language: String,
        onResult: (CardScannerResult) -> Unit,
    ): NativeScannerSession = VisionScannerSession(camera, config, language, logger, onResult)
}
