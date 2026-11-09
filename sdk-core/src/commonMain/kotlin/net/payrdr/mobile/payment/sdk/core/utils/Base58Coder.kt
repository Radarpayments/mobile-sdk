package net.payrdr.mobile.payment.sdk.core.utils

import net.payrdr.mobile.payment.sdk.core.InternalPayrdrApi

/**
 * Utilities for Base58 encoding / decoding (Bitcoin alphabet, excluding `0`, `O`, `I`, `l`).
 *
 * Implementation is `BigInteger`-free to stay KMP-compatible. See the Bitcoin wiki reference
 * algorithm: https://en.bitcoin.it/wiki/Base58Check_encoding#Creating_a_Base58Check_string
 */
@InternalPayrdrApi
public object Base58Coder {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private const val BASE = 58
    private const val BYTE_255 = 255
    private const val BASE_256 = 256

    public fun encode(input: String): String {
        val inputBytes = input.encodeToByteArray().map { it.toInt() and BYTE_255 }.toMutableList()
        val leadingZeros = inputBytes.takeWhile { it == 0 }.count()

        val result = StringBuilder()
        while (inputBytes.any { it != 0 }) {
            var remainder = 0
            for (i in inputBytes.indices) {
                val accum = inputBytes[i] + remainder * BASE_256
                inputBytes[i] = accum / BASE
                remainder = accum % BASE
            }
            result.append(ALPHABET[remainder])
        }
        repeat(leadingZeros) { result.append(ALPHABET[0]) }
        return result.reverse().toString()
    }

    public fun decode(input: String): String {
        require(isValidBase58String(input)) { "Input is not a valid Base58 string." }
        val leadingOnes = input.takeWhile { it == ALPHABET[0] }.count()

        val digits = IntArray(input.length)
        var digitsLen = 0
        for (char in input) {
            var carry = ALPHABET.indexOf(char)
            for (i in 0 until digitsLen) {
                carry += digits[i] * BASE
                digits[i] = carry and BYTE_255
                carry = carry shr 8
            }
            while (carry > 0) {
                digits[digitsLen++] = carry and BYTE_255
                carry = carry shr 8
            }
        }

        val output = ByteArray(leadingOnes + digitsLen)
        for (i in 0 until digitsLen) {
            output[leadingOnes + (digitsLen - 1 - i)] = digits[i].toByte()
        }
        return output.decodeToString()
    }

    public fun isValidBase58String(input: String): Boolean = input.all { it in ALPHABET }
}
