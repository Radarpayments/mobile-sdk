package net.payrdr.mobile.payment.sdk.payment.http

internal actual class PlatformLock actual constructor() {
    actual fun <T> withLock(block: () -> T): T = synchronized(this) { block() }
}
