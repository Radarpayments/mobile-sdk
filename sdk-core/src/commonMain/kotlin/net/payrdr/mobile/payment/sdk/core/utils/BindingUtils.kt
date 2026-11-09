package net.payrdr.mobile.payment.sdk.core.utils

private const val PREFIX = "pm_"

/**
 * Utilities for parsing the stored-payment-method identifier format `pm_{base58(uuid)}`.
 */
internal object BindingUtils {
    fun extractBindingId(paymentMethodId: String): String {
        if (!isValidPaymentMethodId(paymentMethodId)) return ""
        val body = paymentMethodId.substring(PREFIX.length)
        return Base58Coder.decode(body).trim()
    }

    private fun isValidPaymentMethodId(paymentMethodId: String): Boolean {
        if (!paymentMethodId.startsWith(PREFIX)) return false
        val body = paymentMethodId.substring(PREFIX.length)
        return Base58Coder.isValidBase58String(body)
    }
}
