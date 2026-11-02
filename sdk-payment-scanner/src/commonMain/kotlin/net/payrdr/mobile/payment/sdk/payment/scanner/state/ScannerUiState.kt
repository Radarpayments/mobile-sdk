package net.payrdr.mobile.payment.sdk.payment.scanner.state

import net.payrdr.mobile.payment.sdk.payment.scanner.ScannedCard

public sealed interface ScannerUiState {
    public data object Initializing : ScannerUiState

    public data object PermissionDenied : ScannerUiState

    public data object Scanning : ScannerUiState

    public data class Detecting(
        val candidatePan: String,
    ) : ScannerUiState

    public data class Success(
        val card: ScannedCard,
    ) : ScannerUiState

    public data object Timeout : ScannerUiState

    public data object CameraUnavailable : ScannerUiState

    public data object Cancelled : ScannerUiState
}
