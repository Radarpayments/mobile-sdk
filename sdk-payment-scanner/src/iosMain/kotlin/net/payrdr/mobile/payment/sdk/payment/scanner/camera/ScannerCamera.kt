package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import net.payrdr.mobile.payment.sdk.payment.presentation.PayrdrUiApi
import platform.UIKit.UIView

/**
 * The camera and text recognition the scanner runs on. The payment framework holds no capture code: the
 * `RadarpaymentsSDKScanner` Swift module implements this, so an app without the scanner links no camera API.
 */
@PayrdrUiApi
public interface ScannerCamera {
    /** Whether the app may use the camera, without asking. */
    public fun access(): ScannerCameraAccess

    /** Asks the buyer for the camera; [onAnswer] comes once, on any thread. */
    public fun requestAccess(onAnswer: (Boolean) -> Unit)

    /**
     * Prepares a camera run. [onText] gets the lines read from a frame, on a background thread, only when there are
     * some; [onError] is called when the camera cannot be opened.
     */
    public fun open(
        onText: (List<ScannerTextLine>) -> Unit,
        onError: () -> Unit,
    ): ScannerCameraFeed
}

/** The camera permission as the scanner tells it apart. */
@PayrdrUiApi
public enum class ScannerCameraAccess {
    Granted,
    Denied,
    NotAsked,
}

/** One camera run: it starts once and stops for good. */
@PayrdrUiApi
public interface ScannerCameraFeed {
    /** The view showing what the camera sees. */
    public val preview: UIView

    /** Opens the camera; later calls do nothing. */
    public fun start()

    /** Closes the camera for good. */
    public fun stop()

    /** Switches the flashlight, when the device has one. */
    public fun setTorch(on: Boolean)
}

/** A line of text in a frame; the box is in fractions of the frame, origin at the top left. */
@PayrdrUiApi
public class ScannerTextLine(
    public val text: String,
    public val left: Float,
    public val top: Float,
    public val right: Float,
    public val bottom: Float,
)
