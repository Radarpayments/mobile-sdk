package net.payrdr.mobile.payment.sdk.payment.ui.theme

/**
 * Colour palette for the SDK checkout UI. Mirrors a subset of Material 3 `ColorScheme` but
 * exposes only the knobs that the merchant can usefully override.
 *
 * @property primary brand accent — Pay button, selected payment method, focused field outlines
 * @property onPrimary foreground painted on top of [primary]
 * @property background sheet / screen background, text-field fill
 * @property onBackground primary text on [background]
 * @property surface secondary fill — saved-card plate, ghost buttons, amount chip
 * @property onSurface primary text on [surface]
 * @property error error icons, error outlines, declined-payment tint
 * @property onError foreground on [error]
 * @property outline hairline dividers, unfocused field borders
 * @property success success-state tint (used by ResultScreen)
 * @property onSurfaceVariant secondary text — field labels, helper text, merchant name; [onBackground] at 70 % by default
 * @property placeholder placeholder text and tertiary icons; [onBackground] at 50 % by default
 * @property outlineStrong hover/pressed borders, radio rings, switch track when off; [outline] by default
 * @property scrim dimming layer behind the payment sheet; black at 45 % by default
 */
public data class PaymentColors(
    val primary: PaymentColor,
    val onPrimary: PaymentColor,
    val background: PaymentColor,
    val onBackground: PaymentColor,
    val surface: PaymentColor,
    val onSurface: PaymentColor,
    val error: PaymentColor,
    val onError: PaymentColor,
    val outline: PaymentColor,
    val success: PaymentColor,
    val onSurfaceVariant: PaymentColor = onBackground.withAlpha(SECONDARY_TEXT_ALPHA),
    val placeholder: PaymentColor = onBackground.withAlpha(PLACEHOLDER_ALPHA),
    val outlineStrong: PaymentColor = outline,
    val scrim: PaymentColor = PaymentColor.Black.withAlpha(SCRIM_ALPHA),
)

/** A palette is dark when its background's luma is below one half: picks the system bar icons and wallet buttons. */
internal val PaymentColors.isDark: Boolean
    get() {
        val c = background
        val luma = (LUMA_RED * c.red + LUMA_GREEN * c.green + LUMA_BLUE * c.blue) / CHANNEL_MAX
        return luma < DARK_LUMA
    }

private const val SECONDARY_TEXT_ALPHA = 0.7f
private const val PLACEHOLDER_ALPHA = 0.5f
private const val SCRIM_ALPHA = 0.45f
private const val LUMA_RED = 0.299f
private const val LUMA_GREEN = 0.587f
private const val LUMA_BLUE = 0.114f
private const val CHANNEL_MAX = 255f
private const val DARK_LUMA = 0.5f
