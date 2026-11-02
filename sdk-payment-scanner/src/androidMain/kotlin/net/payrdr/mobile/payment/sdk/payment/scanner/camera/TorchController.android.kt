package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import androidx.camera.core.Camera

/**
 * Android actual. A non-expect [attach] method captures the live CameraX `Camera` handle produced
 * by `bindToLifecycle`; [setEnabled] then forwards to `CameraControl.enableTorch`.
 */
public actual class TorchController actual constructor() {
    @Volatile private var camera: Camera? = null

    public fun attach(camera: Camera?) {
        this.camera = camera
    }

    public actual fun setEnabled(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
    }

    public actual fun isAvailable(): Boolean = camera?.cameraInfo?.hasFlashUnit() == true
}
