package net.payrdr.mobile.payment.sdk.payment.ui.theme

/**
 * Corners of the SDK UI surfaces.
 *
 * - [field] — text-field background
 * - [button] — pay button, wallet buttons, ghost buttons
 * - [sheet] — bottom-sheet container; only its top corners are rounded, and [PaymentCorners.Pill] keeps the default
 * - [card] — payment-method rows, saved-card plate, receipt
 */
public data class PaymentShapes(
    val field: PaymentCorners,
    val button: PaymentCorners,
    val sheet: PaymentCorners,
    val card: PaymentCorners,
) {
    /** The SDK's default corners. */
    public companion object {
        /** The SDK's own corners. */
        public val Default: PaymentShapes =
            PaymentShapes(
                field = PaymentCorners.Radius(12f),
                button = PaymentCorners.Radius(14f),
                sheet = PaymentCorners.Radius(26f),
                card = PaymentCorners.Radius(14f),
            )
    }
}

/** How round the corners of a surface are. */
public sealed interface PaymentCorners {
    /**
     * Corners of one radius.
     *
     * @property points the radius in points on iOS and in dp on Android
     */
    public data class Radius(
        val points: Float,
    ) : PaymentCorners {
        init {
            require(points >= 0f) { "points must not be negative, got $points" }
        }
    }

    /** Fully rounded ends: the radius is half the surface's height. */
    public data object Pill : PaymentCorners
}

/** The sheet's top-corner radius: a pill is no shape for a sheet, so it keeps the default. */
internal val PaymentShapes.sheetRadius: Float
    get() = (sheet as? PaymentCorners.Radius ?: PaymentShapes.Default.sheet as PaymentCorners.Radius).points
