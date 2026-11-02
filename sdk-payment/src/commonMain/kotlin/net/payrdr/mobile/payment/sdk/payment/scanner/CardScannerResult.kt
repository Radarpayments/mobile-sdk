package net.payrdr.mobile.payment.sdk.payment.scanner

/** Terminal outcome of a scanner screen run. */
public sealed interface CardScannerResult {
    public data class Success(
        val card: ScannedCard,
    ) : CardScannerResult

    /** User tapped close, "Enter manually", or the system back. */
    public data object Cancelled : CardScannerResult

    /** User refused camera permission in the system dialog. */
    public data object PermissionDenied : CardScannerResult

    public data class Failed(
        val reason: Reason,
    ) : CardScannerResult

    public enum class Reason { Timeout, CameraUnavailable, Unknown }
}
