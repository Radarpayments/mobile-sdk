package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Full-frame camera preview hosting the OCR pipeline. Each detected-text frame calls [onRecognized]
 * with a list of normalised lines. The recognizer is owned by the composable and torn down on
 * `DisposableEffect` `onDispose`.
 *
 * Platform behaviour:
 *  - Android: `AndroidView` + CameraX `PreviewView` with `ImageAnalysis` configured with
 *    `STRATEGY_KEEP_ONLY_LATEST`. Analyser posts results back via [onRecognized] on the main thread.
 *  - iOS: `UIKitView` + `AVCaptureSession` + `AVCaptureVideoDataOutput` with
 *    `alwaysDiscardsLateVideoFrames = true`, processing on a serial dispatch queue,
 *    marshalling results onto the main thread.
 *
 * On camera startup failure, [onError] fires.
 */
@Composable
public expect fun CameraPreview(
    onRecognized: (List<RecognizedLine>) -> Unit,
    onError: () -> Unit,
    torch: TorchState,
    modifier: Modifier,
)

/** UI-controlled torch state. The platform wires this to its TorchController internally. */
public data class TorchState(
    val enabled: Boolean,
)
