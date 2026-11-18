package net.payrdr.mobile.payment.sdk.qa.intake

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Opens Google's code scanner for one QR code and returns its text, for [intakeFromText]. Returns null when the tester
 * cancels, when Play services cannot provide the scanner, or when the code carries no text; it never throws.
 * [context] should be the current activity, which the scanner's screen is started from.
 */
internal suspend fun scanQr(context: Context): String? =
    suspendCancellableCoroutine { continuation ->
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        // Play services reports a missing or outdated module through the task, but a broken installation can also throw
        // here, and the scan button must not crash the app.
        val scan =
            try {
                GmsBarcodeScanning.getClient(context, options).startScan()
            } catch (_: RuntimeException) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
        scan
            .addOnSuccessListener { barcode -> if (continuation.isActive) continuation.resume(barcode.rawValue) }
            .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
            .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
    }
