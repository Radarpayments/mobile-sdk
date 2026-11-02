package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

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
 * @property onSurfaceVariant secondary text — field labels, helper text, merchant name
 * @property placeholder placeholder text and tertiary icons
 * @property outlineStrong hover/pressed borders, radio rings, switch track when off
 * @property scrim dimming layer behind the payment sheet
 */
@Immutable
public data class PaymentColors(
    val primary: Color,
    val onPrimary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val error: Color,
    val onError: Color,
    val outline: Color,
    val success: Color,
    val onSurfaceVariant: Color = onBackground.copy(alpha = 0.7f),
    val placeholder: Color = onBackground.copy(alpha = 0.5f),
    val outlineStrong: Color = outline,
    val scrim: Color = Color.Black.copy(alpha = 0.45f),
)

/** [PaymentColors.primary] tinted onto the background — selected payment-method fill, icon plates. */
internal val PaymentColors.primarySoft: Color get() = lerp(background, primary, 0.11f)

/**
 * Fill of secondary buttons: the text colour tinted onto the background, so the button stands out from the sheet in any
 * merchant palette (a [PaymentColors.surface] close to the background would leave it all but invisible).
 */
internal val PaymentColors.secondaryFill: Color get() = lerp(background, onBackground, 0.09f)

/** Success tint for the result icon plate. */
internal val PaymentColors.successSoft: Color get() = lerp(background, success, 0.14f)

/** Error tint for the result icon plate and the action-code chip. */
internal val PaymentColors.errorSoft: Color get() = lerp(background, error, 0.12f)
