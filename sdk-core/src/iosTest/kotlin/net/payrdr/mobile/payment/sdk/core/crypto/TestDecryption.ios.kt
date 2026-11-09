@file:OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)

package net.payrdr.mobile.payment.sdk.core.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecKeyCreateDecryptedData
import platform.Security.SecKeyCreateWithData
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPrivate
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecKeyAlgorithmRSAEncryptionPKCS1
import platform.posix.memcpy
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Security.framework reads an RSA private key as PKCS#1; PKCS#8 wraps it after a fixed header for a 2048-bit key.
private const val PKCS8_RSA_HEADER = 26

internal actual fun decryptWithTestKey(ciphertext: ByteArray): ByteArray {
    val pkcs1 = Base64.decode(TestRsaKeys.PRIVATE_KEY_PKCS8_BASE64).let { it.copyOfRange(PKCS8_RSA_HEADER, it.size) }
    val attrs = CFDictionaryCreateMutable(kCFAllocatorDefault, 2, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
    CFDictionarySetValue(attrs, kSecAttrKeyType, kSecAttrKeyTypeRSA)
    CFDictionarySetValue(attrs, kSecAttrKeyClass, kSecAttrKeyClassPrivate)
    val keyData = pkcs1.usePinned { CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), pkcs1.size.convert()) }
    val key = checkNotNull(SecKeyCreateWithData(keyData, attrs as CFDictionaryRef, null)) { "test private key not read" }
    val input = ciphertext.usePinned { CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), ciphertext.size.convert()) }
    val output = checkNotNull(SecKeyCreateDecryptedData(key, kSecKeyAlgorithmRSAEncryptionPKCS1, input, null)) { "not decrypted" }
    val result = ByteArray(CFDataGetLength(output).toInt())
    result.usePinned { memcpy(it.addressOf(0), CFDataGetBytePtr(output), result.size.convert()) }
    listOf(output, input, key, keyData, attrs).forEach { CFRelease(it) }
    return result
}
