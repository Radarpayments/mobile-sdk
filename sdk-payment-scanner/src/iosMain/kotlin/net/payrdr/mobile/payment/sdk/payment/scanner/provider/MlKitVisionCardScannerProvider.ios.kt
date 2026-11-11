package net.payrdr.mobile.payment.sdk.payment.scanner.provider

import net.payrdr.mobile.payment.sdk.payment.logger.LogTransport
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.presentation.NativeCardScanner
import net.payrdr.mobile.payment.sdk.payment.presentation.NativeScannerSession
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import net.payrdr.mobile.payment.sdk.payment.scanner.presentation.VisionScannerSession

/**
 * Default [CardScannerProvider] shipping Vision camera OCR, shown by the checkout's SwiftUI scanner screen; see the
 * common declaration.
 */
public actual class MlKitVisionCardScannerProvider actual constructor(
    logTransport: LogTransport?,
) : CardScannerProvider,
    NativeCardScanner {
    internal actual val logger: Logger? = logTransport?.let { Logger(transports = listOf(it)) }

    override fun openScanner(
        config: CardScannerConfig,
        language: String,
        onResult: (CardScannerResult) -> Unit,
    ): NativeScannerSession = VisionScannerSession(config, language, logger, onResult)
}
