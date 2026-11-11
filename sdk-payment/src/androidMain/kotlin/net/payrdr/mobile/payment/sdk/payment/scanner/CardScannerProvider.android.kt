package net.payrdr.mobile.payment.sdk.payment.scanner

import androidx.compose.runtime.Composable

/** Supplies a full-screen card scanner Composable; see the common declaration for the contract. */
public actual interface CardScannerProvider {
    /** The scanner screen; it MUST eventually invoke [onResult] exactly once. */
    @Composable
    public fun CardScannerScreen(
        config: CardScannerConfig,
        onResult: (CardScannerResult) -> Unit,
    )
}
