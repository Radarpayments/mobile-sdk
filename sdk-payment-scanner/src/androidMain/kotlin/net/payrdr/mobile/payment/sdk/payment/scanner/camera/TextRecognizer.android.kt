package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal actual class TextRecognizer actual constructor() {
    private val client = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @SuppressLint("UnsafeOptInUsageError")
    actual suspend fun recognize(frame: Any): List<RecognizedLine> {
        val proxy = frame as? ImageProxy ?: return emptyList()
        val media =
            proxy.image ?: run {
                proxy.close()
                return emptyList()
            }
        val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        val w = input.width.toFloat().coerceAtLeast(1f)
        val h = input.height.toFloat().coerceAtLeast(1f)
        return try {
            suspendCancellableCoroutine { cont ->
                client
                    .process(input)
                    .addOnSuccessListener { visionText ->
                        cont.resume(
                            visionText.textBlocks.flatMap { block ->
                                block.lines.map { line ->
                                    val box = line.boundingBox
                                    val rect =
                                        if (box != null) {
                                            NormalisedRect(
                                                left = box.left / w,
                                                top = box.top / h,
                                                right = box.right / w,
                                                bottom = box.bottom / h,
                                            )
                                        } else {
                                            NormalisedRect(0f, 0f, 1f, 1f)
                                        }
                                    RecognizedLine(line.text, rect)
                                }
                            },
                        )
                    }.addOnFailureListener { cont.resumeWithException(it) }
                    .addOnCanceledListener { cont.resume(emptyList()) }
            }
        } finally {
            proxy.close()
        }
    }

    actual fun close(): Unit = client.close()
}
