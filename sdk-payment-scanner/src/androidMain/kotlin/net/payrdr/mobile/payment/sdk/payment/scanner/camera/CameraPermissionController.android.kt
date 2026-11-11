package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import net.payrdr.mobile.payment.sdk.payment.scanner.util.ContextHolder
import net.payrdr.mobile.payment.sdk.payment.scanner.util.requireScannerActivity
import kotlin.coroutines.resume

/**
 * Checks the permission against the application context stored by
 * [net.payrdr.mobile.payment.sdk.payment.scanner.util.ScannerInitializer] (auto-registered via
 * `androidx.startup`), and asks for it from the `ComponentActivity` on screen, which the same initializer tracks.
 */
internal class CameraPermissionController {
    private val appContext: Context get() = ContextHolder.current()

    fun status(): CameraPermissionStatus {
        val granted =
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        return if (granted) CameraPermissionStatus.Granted else CameraPermissionStatus.NotDetermined
    }

    suspend fun request(): CameraPermissionStatus {
        val activity = requireScannerActivity()
        return suspendCancellableCoroutine { cont ->
            val key = "sdk_payment_scanner_camera_${System.currentTimeMillis()}"
            lateinit var launcher: ActivityResultLauncher<String>
            launcher =
                activity.activityResultRegistry.register(
                    key,
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    launcher.unregister()
                    if (cont.isActive) {
                        cont.resume(if (granted) CameraPermissionStatus.Granted else CameraPermissionStatus.Denied)
                    }
                }
            launcher.launch(Manifest.permission.CAMERA)
            cont.invokeOnCancellation { launcher.unregister() }
        }
    }

    fun openSettings() {
        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", appContext.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }
}
