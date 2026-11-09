package net.payrdr.mobile.payment.sdk.payment.scanner.camera

internal enum class CameraPermissionStatus { Granted, Denied, NotDetermined }

/**
 * Platform bridge around camera runtime-permission APIs.
 *
 * Android: backs onto `ActivityResultContracts.RequestPermission` and
 * `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.
 *
 * iOS: backs onto `AVCaptureDevice.authorizationStatus(for:)` / `requestAccess(for:)` /
 * `UIApplication.openSettingsURLString`.
 */
internal expect class CameraPermissionController() {
    fun status(): CameraPermissionStatus

    suspend fun request(): CameraPermissionStatus

    fun openSettings()
}
