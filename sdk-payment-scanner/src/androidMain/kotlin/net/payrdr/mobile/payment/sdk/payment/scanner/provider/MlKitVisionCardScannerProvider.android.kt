package net.payrdr.mobile.payment.sdk.payment.scanner.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import net.payrdr.mobile.payment.sdk.payment.logger.LogTransport
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.CameraPermissionController
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.TextRecognizer
import net.payrdr.mobile.payment.sdk.payment.scanner.ui.CardScannerScreen

/**
 * Default [CardScannerProvider] shipping ML Kit camera OCR; see the common declaration.
 * Instantiated by merchants who want camera scanning and passed to `SdkPaymentConfig.cardScannerProvider`.
 *
 * @param logTransport optional sink for scanner events (`event=...`, tag `"CardScanner"`), usually the one of
 *   `SdkPaymentConfig.logTransport`, so scanner events flow through the same sink.
 */
public actual class MlKitVisionCardScannerProvider actual constructor(
    logTransport: LogTransport?,
) : CardScannerProvider {
    internal actual val logger: Logger? = logTransport?.let { Logger(transports = listOf(it)) }

    @Composable
    public override fun CardScannerScreen(
        config: CardScannerConfig,
        onResult: (CardScannerResult) -> Unit,
    ) {
        val recognizer = remember { TextRecognizer() }
        val permissions = remember { CameraPermissionController() }
        // Close recognizer if the provider's call tree leaves composition before CardScannerScreen's
        // own DisposableEffect fires (e.g., if the screen throws before mounting).
        DisposableEffect(recognizer) { onDispose { recognizer.close() } }
        net.payrdr.mobile.payment.sdk.payment.scanner.ui.CardScannerScreen(
            config = config,
            permissions = permissions,
            recognizer = recognizer,
            logger = logger,
            onResult = onResult,
        )
    }
}
