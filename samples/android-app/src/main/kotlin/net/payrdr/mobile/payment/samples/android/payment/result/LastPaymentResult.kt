package net.payrdr.mobile.payment.samples.android.payment.result

import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult

/**
 * Process-scoped holder: the latest CheckoutResult captured by a sub-screen launcher, consumed
 * once by `PaymentResultScreen`. Writer: sub-screen `onResult` callback. Reader: result screen.
 * Cleared on read to prevent stale displays.
 */
object LastPaymentResult {
    private var value: CheckoutResult? = null
    private var startedAt: Long = 0L
    private var methodTag: String = "CARD"

    fun begin(method: String) {
        value = null
        startedAt = System.currentTimeMillis()
        methodTag = method
    }

    fun set(r: CheckoutResult) {
        value = r
    }

    fun readAndClear(): Triple<CheckoutResult?, Long, String>? {
        val v = value ?: return null
        val dur = System.currentTimeMillis() - startedAt
        val m = methodTag
        value = null
        return Triple(v, dur, m)
    }
}
