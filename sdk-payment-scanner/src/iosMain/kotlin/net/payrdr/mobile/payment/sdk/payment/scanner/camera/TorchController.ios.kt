package net.payrdr.mobile.payment.sdk.payment.scanner.camera

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.hasTorch
import platform.AVFoundation.setTorchMode

@OptIn(ExperimentalForeignApi::class)
internal actual class TorchController actual constructor() {
    @kotlin.concurrent.Volatile private var device: AVCaptureDevice? = null

    fun attach(device: AVCaptureDevice?) {
        this.device = device
    }

    actual fun setEnabled(on: Boolean) {
        val dev = device ?: return
        if (!dev.hasTorch) return
        if (dev.lockForConfiguration(null)) {
            dev.setTorchMode(if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff)
            dev.unlockForConfiguration()
        }
    }

    actual fun isAvailable(): Boolean = device?.hasTorch == true
}
