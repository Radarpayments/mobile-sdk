package net.payrdr.mobile.payment.sdk.payment.scanner.camera

/**
 * Axis-aligned rectangle, origin top-left, in normalised [0f, 1f] preview coordinates.
 * Platform adapters must convert their native representation (ML Kit pixel top-left,
 * Vision bottom-left 0..1) into this shape before returning [RecognizedLine]s.
 */
internal data class NormalisedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/**
 * Single line of text detected by the platform OCR, with its bounding box in the preview
 * coordinate system. The parser relies on spatial relationships (expiry is usually below PAN)
 * so the bounding box must be reliable.
 */
internal data class RecognizedLine(
    val text: String,
    val boundingBox: NormalisedRect,
)

/**
 * Suspending OCR adapter. `recognize` takes an opaque platform-supplied frame handle
 * (CameraX `ImageProxy` on Android, `CMSampleBuffer` on iOS) and returns lines with
 * normalised boxes.
 *
 * Implementations dispatch onto a background thread internally; callers may invoke
 * from any coroutine context.
 */
internal expect class TextRecognizer() {
    suspend fun recognize(frame: Any): List<RecognizedLine>

    fun close()
}
