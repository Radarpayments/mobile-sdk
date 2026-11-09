package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import androidx.camera.core.Camera

/**
 * Android actual. A non-expect [attach] method captures the live CameraX `Camera` handle produced
 * by `bindToLifecycle`; [setEnabled] then forwards to `CameraControl.enableTorch`.
 */
internal actual class TorchController actual constructor() {
    @Volatile private var camera: Camera? = null

    fun attach(camera: Camera?) {
        this.camera = camera
    }

    actual fun setEnabled(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
    }

    actual fun isAvailable(): Boolean = camera?.cameraInfo?.hasFlashUnit() == true
}
