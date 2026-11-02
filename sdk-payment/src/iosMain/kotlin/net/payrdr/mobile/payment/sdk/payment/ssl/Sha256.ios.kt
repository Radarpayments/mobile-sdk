package net.payrdr.mobile.payment.sdk.payment.ssl

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
internal actual fun sha256(data: ByteArray): ByteArray {
    val out = ByteArray(CC_SHA256_DIGEST_LENGTH)
    // CC_SHA256 with a null data pointer is undefined on older CC versions; always pass a valid pointer.
    val src = if (data.isEmpty()) ByteArray(1) else data
    src.usePinned { pinned ->
        out.usePinned { outP ->
            CC_SHA256(pinned.addressOf(0), data.size.convert(), outP.addressOf(0).reinterpret<UByteVar>())
        }
    }
    return out
}
