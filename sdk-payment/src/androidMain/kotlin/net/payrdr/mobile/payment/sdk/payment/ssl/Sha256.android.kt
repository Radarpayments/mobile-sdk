package net.payrdr.mobile.payment.sdk.payment.ssl

import java.security.MessageDigest

internal actual fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)
