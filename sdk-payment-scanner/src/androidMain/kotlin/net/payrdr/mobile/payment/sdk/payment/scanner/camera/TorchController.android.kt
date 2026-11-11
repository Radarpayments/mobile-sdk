package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import androidx.camera.core.Camera

/**
 * [attach] takes the live CameraX `Camera` handle produced by `bindToLifecycle`; [setEnabled] then forwards to
 * `CameraControl.enableTorch`.
 */
internal class TorchController {
    @Volatile private var camera: Camera? = null

    fun attach(camera: Camera?) {
        this.camera = camera
    }

    fun setEnabled(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
    }

    fun isAvailable(): Boolean = camera?.cameraInfo?.hasFlashUnit() == true
}
