@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package net.payrdr.mobile.payment.sdk.core.crypto

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyIsAlgorithmSupported
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeySizeInBits
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecKeyAlgorithmRSAEncryptionPKCS1
import platform.Security.kSecKeyOperationTypeEncrypt

public actual class Rsa actual constructor() {
    public actual fun encryptPkcs1(
        data: ByteArray,
        publicKeyDer: ByteArray,
    ): ByteArray {
        val keySizeBits = publicKeyDer.size * BITS_PER_BYTE
        return memScoped {
            // Build CFMutableDictionary attribute dict using CF APIs (no NSCopying cast needed).
            val attrs: CFMutableDictionaryRef =
                CFDictionaryCreateMutable(
                    kCFAllocatorDefault,
                    3,
                    kCFTypeDictionaryKeyCallBacks.ptr,
                    kCFTypeDictionaryValueCallBacks.ptr,
                ) ?: throw IllegalStateException("Failed to create attribute dictionary")

            CFDictionarySetValue(attrs, kSecAttrKeyType, kSecAttrKeyTypeRSA)
            CFDictionarySetValue(attrs, kSecAttrKeyClass, kSecAttrKeyClassPublic)

            val keySizeVar = alloc<IntVar>()
            keySizeVar.value = keySizeBits
            val keySizeCf =
                CFNumberCreate(kCFAllocatorDefault, kCFNumberSInt32Type, keySizeVar.ptr)
                    ?: throw IllegalStateException("Failed to create CFNumber for key size")
            CFDictionarySetValue(attrs, kSecAttrKeySizeInBits, keySizeCf)
            CFRelease(keySizeCf)

            // Create CFData for the DER-encoded public key bytes (avoids NSData bridging).
            val keyDataCf: CFDataRef =
                publicKeyDer.usePinned { pinned ->
                    CFDataCreate(
                        kCFAllocatorDefault,
                        pinned.addressOf(0).reinterpret(),
                        publicKeyDer.size.convert(),
                    )
                } ?: throw IllegalArgumentException("Failed to create CFData for key")

            val keyRef =
                SecKeyCreateWithData(
                    keyData = keyDataCf,
                    attributes = attrs as CFDictionaryRef,
                    error = null,
                ) ?: run {
                    CFRelease(keyDataCf)
                    CFRelease(attrs)
                    throw IllegalArgumentException("Failed to parse public key")
                }
            CFRelease(keyDataCf)
            CFRelease(attrs)

            if (!SecKeyIsAlgorithmSupported(
                    keyRef,
                    kSecKeyOperationTypeEncrypt,
                    kSecKeyAlgorithmRSAEncryptionPKCS1,
                )
            ) {
                CFRelease(keyRef)
                throw IllegalStateException("Algorithm rsaEncryptionPKCS1 not supported")
            }

            // Create CFData for the plaintext bytes.
            val plainTextCf: CFDataRef =
                data.usePinned { pinned ->
                    CFDataCreate(
                        kCFAllocatorDefault,
                        pinned.addressOf(0).reinterpret(),
                        data.size.convert(),
                    )
                } ?: run {
                    CFRelease(keyRef)
                    throw IllegalStateException("Failed to create CFData for plaintext")
                }

            val errorVar = alloc<CFErrorRefVar>()
            val encryptedCf =
                SecKeyCreateEncryptedData(
                    key = keyRef,
                    algorithm = kSecKeyAlgorithmRSAEncryptionPKCS1,
                    plaintext = plainTextCf,
                    error = errorVar.ptr,
                ) ?: run {
                    CFRelease(plainTextCf)
                    CFRelease(keyRef)
                    throw IllegalStateException("RSA encryption failed (CFError=${errorVar.value})")
                }
            CFRelease(plainTextCf)
            CFRelease(keyRef)

            // Read ciphertext bytes from CFDataRef directly (no NSData bridge needed).
            val encLen = CFDataGetLength(encryptedCf).toInt()
            val result = ByteArray(encLen)
            if (encLen > 0) {
                val bytePtr =
                    CFDataGetBytePtr(encryptedCf)
                        ?: throw IllegalStateException("Failed to get encrypted data bytes")
                result.usePinned { pinned ->
                    platform.posix.memcpy(
                        pinned.addressOf(0),
                        bytePtr,
                        encLen.convert(),
                    )
                }
            }
            CFRelease(encryptedCf)
            result
        }
    }

    private companion object {
        const val BITS_PER_BYTE = 8
    }
}
