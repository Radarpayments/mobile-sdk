package net.payrdr.mobile.payment.sdk.payment.ui.theme

/**
 * An sRGB colour of the checkout theme.
 *
 * Build it from a hex literal, `PaymentColor(0xFF1F4FD8)` (Swift: `PaymentColor(argb: 0xFF1F4FD8)`), or from components
 * with [rgb]. On Android, `Color.toPaymentColor()` converts a Compose colour.
 *
 * @property argb the colour as `0xAARRGGBB`
 */
public data class PaymentColor(
    val argb: Long,
) {
    init {
        require(argb in 0..MAX_ARGB) { "argb must be 0xAARRGGBB, got $argb" }
    }

    /** The alpha channel, 0–255. */
    public val alpha: Int get() = channel(ALPHA_SHIFT)

    /** The red channel, 0–255. */
    public val red: Int get() = channel(RED_SHIFT)

    /** The green channel, 0–255. */
    public val green: Int get() = channel(GREEN_SHIFT)

    /** The blue channel, 0–255. */
    public val blue: Int get() = channel(0)

    /**
     * This colour with its alpha replaced by [alpha] (0–1, rounded to the nearest of 256 steps the way Compose's
     * `Color.copy(alpha = …)` rounds it).
     */
    public fun withAlpha(alpha: Float): PaymentColor {
        require(alpha in 0f..1f) { "alpha must be in 0..1, got $alpha" }
        return PaymentColor(argb and RGB_MASK or (alpha.toChannel().toLong() shl ALPHA_SHIFT))
    }

    override fun toString(): String = "PaymentColor(#${argb.toString(HEX).uppercase().padStart(ARGB_DIGITS, '0')})"

    private fun channel(shift: Int): Int = ((argb shr shift) and CHANNEL_MASK).toInt()

    /** Common colours and factories. */
    public companion object {
        /** Opaque black. */
        public val Black: PaymentColor = PaymentColor(0xFF000000)

        /** Opaque white. */
        public val White: PaymentColor = PaymentColor(0xFFFFFFFF)

        /** The colour of the [red], [green] and [blue] channels (0–255) with [alpha] (0–1). */
        public fun rgb(
            red: Int,
            green: Int,
            blue: Int,
            alpha: Float = 1f,
        ): PaymentColor {
            require(red in CHANNELS && green in CHANNELS && blue in CHANNELS) { "channels must be in 0..255, got $red, $green, $blue" }
            val opaque = PaymentColor(OPAQUE or (red.toLong() shl RED_SHIFT) or (green.toLong() shl GREEN_SHIFT) or blue.toLong())
            return if (alpha == 1f) opaque else opaque.withAlpha(alpha)
        }

        private const val MAX_ARGB = 0xFFFFFFFFL
        private const val RGB_MASK = 0x00FFFFFFL
        private const val OPAQUE = 0xFF000000L
        private const val CHANNEL_MASK = 0xFFL
        private const val ALPHA_SHIFT = 24
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
        private const val HEX = 16
        private const val ARGB_DIGITS = 8
        private const val CHANNEL_MAX = 255f
        private val CHANNELS = 0..255

        private fun Float.toChannel(): Int = (this * CHANNEL_MAX + 0.5f).toInt()
    }
}
