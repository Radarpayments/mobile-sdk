package net.payrdr.mobile.payment.sdk.payment.ui.util

/**
 * [text] as one left-to-right run however the layout around it reads. Card masks, amounts and codes have no
 * letters to set their direction, so in an Arabic layout they would come out reversed (`1111 ••••`, `3.90€`).
 */
internal fun ltr(text: String): String = "⁦$text⁩"
