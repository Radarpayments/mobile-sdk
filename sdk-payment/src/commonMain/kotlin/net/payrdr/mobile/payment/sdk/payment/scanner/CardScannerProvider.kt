package net.payrdr.mobile.payment.sdk.payment.scanner

import androidx.compose.runtime.Composable

/**
 * Supplies a full-screen card scanner Composable. Lives in `sdk-payment` so that `sdk-payment` can
 * depend on the abstraction alone; concrete implementations ship in separate modules (e.g.
 * `sdk-payment-scanner`) so the scanner can be fully omitted from a build.
 *
 * The scanner is responsible for: camera permission, preview, OCR, state machine, and UX.
 * It MUST eventually invoke [onResult] exactly once.
 */
public interface CardScannerProvider {
    @Composable
    public fun CardScannerScreen(
        config: CardScannerConfig,
        onResult: (CardScannerResult) -> Unit,
    )
}
