package net.payrdr.mobile.payment.sdk.payment.scanner.camera

public enum class CameraPermissionStatus { Granted, Denied, NotDetermined }

/**
 * Platform bridge around camera runtime-permission APIs.
 *
 * Android: backs onto `ActivityResultContracts.RequestPermission` and
 * `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`.
 *
 * iOS: backs onto `AVCaptureDevice.authorizationStatus(for:)` / `requestAccess(for:)` /
 * `UIApplication.openSettingsURLString`.
 */
public expect class CameraPermissionController() {
    public fun status(): CameraPermissionStatus

    public suspend fun request(): CameraPermissionStatus

    public fun openSettings()
}
