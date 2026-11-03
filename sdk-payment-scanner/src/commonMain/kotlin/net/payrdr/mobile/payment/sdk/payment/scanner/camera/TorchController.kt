package net.payrdr.mobile.payment.sdk.payment.scanner.camera

internal expect class TorchController() {
    fun setEnabled(on: Boolean)

    fun isAvailable(): Boolean
}
