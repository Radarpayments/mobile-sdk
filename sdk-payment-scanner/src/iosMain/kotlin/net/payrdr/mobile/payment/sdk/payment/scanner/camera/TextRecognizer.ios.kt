package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import cnames.structs.opaqueCMSampleBuffer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.reinterpret
import net.payrdr.mobile.payment.sdk.payment.scanner.vision.payrdr_recognize_text
import platform.Foundation.NSNumber

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual class TextRecognizer actual constructor() {
    /**
     * Runs Vision on the camera frame synchronously. The work lives in Objective-C
     * (`scannervision.def`): Vision objects made from Kotlin would hold the camera buffer until the
     * next Kotlin GC and starve the capture output.
     */
    actual suspend fun recognize(frame: Any): List<RecognizedLine> {
        val buffer = (frame as? CPointer<*>)?.reinterpret<opaqueCMSampleBuffer>() ?: return emptyList()
        return autoreleasepool {
            payrdr_recognize_text(buffer).orEmpty().mapNotNull { item ->
                val line = item as? List<*> ?: return@mapNotNull null
                val text = line.getOrNull(0) as? String ?: return@mapNotNull null
                val (x, y, width, height) = (1..4).map { (line.getOrNull(it) as? NSNumber)?.doubleValue ?: 0.0 }
                RecognizedLine(
                    text = text,
                    boundingBox =
                        NormalisedRect(
                            left = x.toFloat(),
                            top = (1.0 - y - height).toFloat(),
                            right = (x + width).toFloat(),
                            bottom = (1.0 - y).toFloat(),
                        ),
                )
            }
        }
    }

    actual fun close(): Unit = Unit
}
