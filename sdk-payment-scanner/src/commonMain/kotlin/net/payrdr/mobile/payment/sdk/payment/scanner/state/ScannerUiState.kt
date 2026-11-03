package net.payrdr.mobile.payment.sdk.payment.scanner.state

import net.payrdr.mobile.payment.sdk.payment.scanner.ScannedCard

internal sealed interface ScannerUiState {
    data object Initializing : ScannerUiState

    data object PermissionDenied : ScannerUiState

    data object Scanning : ScannerUiState

    data class Detecting(
        val candidatePan: String,
    ) : ScannerUiState

    data class Success(
        val card: ScannedCard,
    ) : ScannerUiState

    data object Timeout : ScannerUiState

    data object CameraUnavailable : ScannerUiState

    data object Cancelled : ScannerUiState
}
