package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetwork
import net.payrdr.mobile.payment.sdk.payment.ui.util.displayName

/**
 * Card network logo in a fixed 40×26 slot, in the network's brand colours. On a dark sheet background the
 * colours that would vanish switch to light ones (Visa, Amex and Discover turn white). With [tint] the logo
 * is drawn monochrome in that colour instead. [CardNetwork.UNKNOWN] reserves the space but draws nothing.
 *
 * @param tint one colour for the whole logo, e.g. on a card picture; unspecified = brand colours, which follow the
 *   sheet's background and so need [net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentThemeProvider] around them
 */
@Composable
internal fun CardNetworkIcon(
    network: CardNetwork,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val slot = modifier.size(width = SLOT_WIDTH, height = 26.dp)
    if (LocalPlaceholder.current) return PlaceholderBlock(slot)
    val logo = colorLogoOf(network)
    if (logo == null) {
        Box(modifier = slot)
        return
    }
    // Only the brand colours depend on the sheet; a tinted logo also works outside PaymentThemeProvider.
    val dark = !tint.isSpecified && LocalPaymentColors.current.background.luminance() < DARK_LUMINANCE
    val mono = monoLogoOf(network)
    val painter =
        remember(network, tint, dark) {
            when {
                // The monochrome sets keep overlaps readable (Mastercard's circles) where a flat tint would merge them.
                tint.isSpecified && mono != null -> MonoLogoPainter(mono, tint)
                else -> ColorLogoPainter(logo, dark, tint)
            }
        }
    // Each mark gets its own height so that wide wordmarks and compact symbols look equally heavy.
    val height = markHeight(network)
    val ratio = painter.intrinsicSize.width / painter.intrinsicSize.height
    val width = minOf(SLOT_WIDTH, height * ratio)
    Box(modifier = slot.semantics { contentDescription = network.displayName }, contentAlignment = Alignment.Center) {
        Image(painter = painter, contentDescription = null, modifier = Modifier.size(width = width, height = width / ratio))
    }
}

private val SLOT_WIDTH = 40.dp

/** Backgrounds darker than this get the logos' light variants. */
private const val DARK_LUMINANCE = 0.4f

private fun colorLogoOf(network: CardNetwork): ColorLogo? =
    when (network) {
        CardNetwork.VISA -> PaymentNetworkColorLogos.Visa
        CardNetwork.MASTERCARD -> PaymentNetworkColorLogos.Mastercard
        CardNetwork.AMEX -> PaymentNetworkColorLogos.Amex
        CardNetwork.UNIONPAY -> PaymentNetworkColorLogos.UnionPay
        CardNetwork.JCB -> PaymentNetworkColorLogos.Jcb
        CardNetwork.DISCOVER -> PaymentNetworkColorLogos.Discover
        CardNetwork.DINERS -> PaymentNetworkColorLogos.Diners
        CardNetwork.MAESTRO -> PaymentNetworkColorLogos.Maestro
        CardNetwork.UNKNOWN -> null
    }

private fun monoLogoOf(network: CardNetwork): MonoLogo? =
    when (network) {
        CardNetwork.VISA -> PaymentNetworkLogos.Visa
        CardNetwork.MASTERCARD -> PaymentNetworkLogos.Mastercard
        CardNetwork.AMEX -> PaymentNetworkLogos.Amex
        CardNetwork.UNIONPAY -> PaymentNetworkLogos.UnionPay
        else -> null
    }

private fun markHeight(network: CardNetwork): Dp =
    when (network) {
        CardNetwork.VISA -> 12.dp
        CardNetwork.MASTERCARD, CardNetwork.MAESTRO -> 20.dp
        CardNetwork.AMEX -> 20.dp
        CardNetwork.UNIONPAY, CardNetwork.JCB -> 22.dp
        CardNetwork.DINERS -> 20.dp
        CardNetwork.DISCOVER -> 8.dp
        CardNetwork.UNKNOWN -> 0.dp
    }
