package net.payrdr.mobile.payment.sdk.qa.intake

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** How a QR scan ended. */
internal sealed interface QrScan {
    /** The code's text, for [intakeFromText]; empty when the code carries bytes that are not text. */
    data class Text(
        val text: String,
    ) : QrScan {
        // A scanned code may carry credentials.
        override fun toString(): String = "Text(…)"
    }

    /** The tester closed the scanner. */
    data object Cancelled : QrScan

    /** Play services could not provide the scanner: missing, outdated, or its module not downloaded yet. */
    data object Unavailable : QrScan
}

/**
 * Opens Google's code scanner for one QR code. It never throws: a scanner that cannot start is [QrScan.Unavailable].
 * [context] should be the current activity, which the scanner's screen is started from.
 */
internal suspend fun scanQr(context: Context): QrScan =
    suspendCancellableCoroutine { continuation ->
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        // Play services reports a missing or outdated module through the task, but a broken installation can also throw
        // here, and the scan button must not crash the app.
        val scan =
            try {
                GmsBarcodeScanning.getClient(context, options).startScan()
            } catch (_: RuntimeException) {
                continuation.resume(QrScan.Unavailable)
                return@suspendCancellableCoroutine
            }
        scan
            .addOnSuccessListener { barcode -> if (continuation.isActive) continuation.resume(QrScan.Text(barcode.rawValue.orEmpty())) }
            .addOnCanceledListener { if (continuation.isActive) continuation.resume(QrScan.Cancelled) }
            .addOnFailureListener { if (continuation.isActive) continuation.resume(QrScan.Unavailable) }
    }
