package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
public actual class CameraPermissionController actual constructor() {
    public actual fun status(): CameraPermissionStatus =
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> CameraPermissionStatus.Granted
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> CameraPermissionStatus.Denied
            AVAuthorizationStatusNotDetermined -> CameraPermissionStatus.NotDetermined
            else -> CameraPermissionStatus.NotDetermined
        }

    public actual suspend fun request(): CameraPermissionStatus =
        suspendCancellableCoroutine { cont ->
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                if (cont.isActive) {
                    cont.resume(if (granted) CameraPermissionStatus.Granted else CameraPermissionStatus.Denied)
                }
            }
        }

    public actual fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>()) {}
    }
}
