package net.payrdr.mobile.payment.sdk.payment.scanner.camera

public expect class TorchController() {
    public fun setEnabled(on: Boolean)

    public fun isAvailable(): Boolean
}
