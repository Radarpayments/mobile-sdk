package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import platform.UIKit.UIView

/**
 * A card scanner the SwiftUI checkout shows without Compose: the provider of `SdkPaymentConfig.cardScannerProvider`
 * conforms to it (the one of sdk-payment-scanner does). A provider that does not gets no scan button.
 */
@PayrdrUiApi
public interface NativeCardScanner {
    /**
     * Starts a scanner run: asks for the camera, then reads the card. [onResult] is called once, on the main thread,
     * when the run ends.
     */
    public fun openScanner(
        config: CardScannerConfig,
        language: String,
        onResult: (CardScannerResult) -> Unit,
    ): NativeScannerSession
}

/** One scanner run as the SwiftUI scanner screen drives it. Every call comes on the main thread. */
@PayrdrUiApi
public interface NativeScannerSession {
    /** The view with the camera's picture; the screen shows it only while the phase is scanning or detected. */
    public val preview: UIView

    /** The scanner's text for [key] in the run's language. */
    public fun text(key: String): String

    /** Calls [onChange] with the current state and every change of it, on the main thread, until [close]. */
    public fun observe(onChange: (NativeScannerState) -> Unit)

    /** Switches the flashlight on or off. */
    public fun toggleTorch()

    /** Ends the run: the result is the pending failure if there is one, a cancellation otherwise. */
    public fun cancel()

    /** Opens the app's page in the system settings, where the camera can be allowed. */
    public fun openSettings()

    /** Stops the camera and the observers; the screen calls it when it goes away. */
    public fun close()
}

/** What the scanner screen draws. */
@PayrdrUiApi
public class NativeScannerState(
    public val phase: NativeScannerPhase,
    public val torchOn: Boolean,
    public val torchButton: Boolean,
)

/** The scanner's phases, as the Compose scanner screen tells them apart. */
@PayrdrUiApi
public enum class NativeScannerPhase {
    Starting,
    PermissionDenied,
    Scanning,
    Detected,
    Success,
    Timeout,
    CameraUnavailable,
    Cancelled,
}
