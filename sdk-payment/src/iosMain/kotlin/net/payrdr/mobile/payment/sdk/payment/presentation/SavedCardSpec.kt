package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.screens.rowSubtitle
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.payment.ui.util.displayName
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import kotlin.math.pow

/**
 * What a row of a saved card shows, as the Compose rows compute it.
 *
 * @property title the short mask, e.g. `•••• 1111`, not yet isolated for right-to-left text
 * @property subtitle network and expiry, or null when the gateway sent neither
 * @property name how the card is called in a sentence and by a screen reader, e.g. `Visa •••• 1111`
 * @property logo the network's mark, or null when the network is not recognised
 */
@PayrdrUiApi
public class SavedCardSpec internal constructor(
    public val title: String,
    public val subtitle: String?,
    public val name: String,
    public val logo: NetworkLogoSpec?,
)

/** The card helpers of the Compose UI for the SwiftUI renderer. */
@PayrdrUiApi
public object SavedCardSpecs {
    /** The row of [card]; [dark] picks the logo colours meant for a dark background. */
    public fun of(
        card: SavedCardInfo,
        dark: Boolean,
    ): SavedCardSpec {
        val network = CardNetworkResolver.resolveSaved(card.paymentSystem, card.maskedPan)
        val mask = CardFormatters.shortMask(card.maskedPan)
        return SavedCardSpec(
            title = mask,
            subtitle = card.rowSubtitle(),
            name = listOf(network.displayName, ltr(mask)).filter { it.isNotBlank() }.joinToString(" "),
            logo = NetworkLogoSpec.of(network, dark),
        )
    }

    /** Whether a background of this `0xAARRGGBB` colour takes the logos' dark variants. */
    public fun isDark(background: Long): Boolean = relativeLuminance(background) < DARK_LUMINANCE

    /** [text] isolated as left-to-right, as card masks and amounts are shown in every language. */
    public fun leftToRight(text: String): String = ltr(text)

    /** A stand-in card of the loading skeleton: its text makes the blocks about as wide as the real lines. */
    public fun skeletonCard(index: Int): SavedCardInfo =
        SavedCardInfo("skeleton-$index", "000000**0000", "VISA", "", canDelete = true, expiry = "00/00")

    // The relative luminance of an sRGB colour, as Compose's `Color.luminance()` gives it to the Android rows.
    private fun relativeLuminance(argb: Long): Double {
        fun linear(shift: Int): Double {
            val channel = ((argb shr shift) and CHANNEL_MASK).toDouble() / CHANNEL_MASK
            return if (channel <= SRGB_KNEE) channel / SRGB_SLOPE else ((channel + SRGB_OFFSET) / (1 + SRGB_OFFSET)).pow(SRGB_GAMMA)
        }
        return RED_WEIGHT * linear(RED_SHIFT) + GREEN_WEIGHT * linear(GREEN_SHIFT) + BLUE_WEIGHT * linear(0)
    }

    // The threshold of the Compose CardNetworkIcon, private there.
    private const val DARK_LUMINANCE = 0.4
    private const val CHANNEL_MASK = 0xFFL
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val RED_WEIGHT = 0.2126
    private const val GREEN_WEIGHT = 0.7152
    private const val BLUE_WEIGHT = 0.0722
    private const val SRGB_KNEE = 0.04045
    private const val SRGB_SLOPE = 12.92
    private const val SRGB_OFFSET = 0.055
    private const val SRGB_GAMMA = 2.4
}
