package net.payrdr.mobile.payment.sdk.payment.ui.theme

/**
 * A text style of the checkout theme. Sizes are in points on iOS and in sp on Android.
 *
 * @property size font size
 * @property weight font weight on the CSS scale, 1–1000: [REGULAR], [MEDIUM], [SEMI_BOLD], [BOLD]
 * @property lineHeight distance between the baselines of a paragraph's lines; `0` keeps the font's own
 * @property letterSpacing extra space between letters; `0` keeps the font's own
 * @property tabularFigures digits of one width, so that amounts and card numbers line up
 * @property fontFamily the font family; `null` keeps the system font (SF Pro on iOS, Roboto on Android)
 */
public data class PaymentTextStyle(
    val size: Float,
    val weight: Int = REGULAR,
    val lineHeight: Float = 0f,
    val letterSpacing: Float = 0f,
    val tabularFigures: Boolean = false,
    val fontFamily: PaymentFontFamily? = null,
) {
    init {
        require(size > 0f) { "size must be positive, got $size" }
        require(weight in WEIGHTS) { "weight must be in 1..1000, got $weight" }
        require(lineHeight >= 0f) { "lineHeight must not be negative, got $lineHeight" }
    }

    /** Common font weights. */
    public companion object {
        /** Regular weight, 400. */
        public const val REGULAR: Int = 400

        /** Medium weight, 500. */
        public const val MEDIUM: Int = 500

        /** Semibold weight, 600. */
        public const val SEMI_BOLD: Int = 600

        /** Bold weight, 700. */
        public const val BOLD: Int = 700

        private val WEIGHTS = 1..1000
    }
}

/**
 * A font family the app ships, for [PaymentTextStyle.fontFamily].
 *
 * On Android it wraps a Compose `FontFamily`: `PaymentFontFamily(FontFamily(Font(R.font.inter)))`. On iOS it names a
 * family the app has registered (listed under `UIAppFonts` or registered with Core Text):
 * `PaymentFontFamily(name: "Inter")`; a name iOS does not know draws in the system font.
 */
public expect class PaymentFontFamily
