package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.ui.components.ColorLogo
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentNetworkColorLogos
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetwork
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.payment.ui.util.displayName

/**
 * A card network's logo in its brand colours for the SwiftUI screens: the shapes the Compose `CardNetworkIcon` draws.
 *
 * @property name the network's name, read by the screen reader.
 * @property height how tall the mark is drawn, in points: wide wordmarks and compact symbols get their own.
 * @property layers the shapes, bottom first, in the logo's own coordinates.
 */
@PayrdrUiApi
public class NetworkLogoSpec internal constructor(
    public val name: String,
    public val height: Double,
    public val layers: List<LogoLayerSpec>,
) {
    /** Finds a network's logo. */
    public companion object {
        /**
         * The logo of the network [maskedPan] belongs to, or null when the network is not recognised. With [dark] the
         * colours that would vanish on a dark background are swapped.
         */
        public fun ofMaskedPan(
            maskedPan: String,
            dark: Boolean,
        ): NetworkLogoSpec? = of(CardNetworkResolver.resolveSaved(paymentSystem = "", maskedPan = maskedPan), dark)

        internal fun of(
            network: CardNetwork,
            dark: Boolean,
        ): NetworkLogoSpec? {
            val logo = colorLogoOf(network) ?: return null
            val colorOf = { argb: Long -> if (dark) logo.onDark[argb] ?: argb else argb }
            return NetworkLogoSpec(
                name = network.displayName,
                height = markHeight(network),
                layers =
                    logo.layers.map { layer ->
                        LogoLayerSpec(
                            path = layer.pathData,
                            color = colorOf(layer.color),
                            gradient =
                                layer.gradient?.let { gradient ->
                                    LogoGradientSpec(
                                        startX = gradient.startX.toDouble(),
                                        startY = gradient.startY.toDouble(),
                                        endX = gradient.endX.toDouble(),
                                        endY = gradient.endY.toDouble(),
                                        offsets = gradient.stops.map { it.first.toDouble() },
                                        colors = gradient.stops.map { colorOf(it.second) },
                                    )
                                },
                        )
                    },
            )
        }
    }
}

/** One shape of a logo: SVG [path] data filled with the ARGB [color], or with [gradient] when it has one. */
@PayrdrUiApi
public class LogoLayerSpec internal constructor(
    public val path: String,
    public val color: Long,
    public val gradient: LogoGradientSpec?,
)

/** A linear gradient in the logo's own coordinates: ARGB [colors] at [offsets] of the way from start to end. */
@PayrdrUiApi
public class LogoGradientSpec internal constructor(
    public val startX: Double,
    public val startY: Double,
    public val endX: Double,
    public val endY: Double,
    public val offsets: List<Double>,
    public val colors: List<Long>,
)

// The two tables below repeat the private ones of the Compose CardNetworkIcon, which this layer does not change.
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

@Suppress("MagicNumber")
private fun markHeight(network: CardNetwork): Double =
    when (network) {
        CardNetwork.VISA -> 12.0
        CardNetwork.MASTERCARD, CardNetwork.MAESTRO -> 20.0
        CardNetwork.AMEX -> 20.0
        CardNetwork.UNIONPAY, CardNetwork.JCB -> 22.0
        CardNetwork.DINERS -> 20.0
        CardNetwork.DISCOVER -> 8.0
        CardNetwork.UNKNOWN -> 0.0
    }
