package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Height of the native Google Pay button: the same as the other checkout buttons. */
internal val WalletButtonHeight: Dp = 54.dp

/**
 * Corner radius of [this] button shape, for native wallet buttons that only take a number;
 * null for shapes that are not rounded rectangles.
 */
internal fun Shape.walletButtonCornerRadius(): Dp? {
    val outline = createOutline(Size(WALLET_PROBE_WIDTH, WalletButtonHeight.value), LayoutDirection.Ltr, Density(1f))
    return (outline as? Outline.Rounded)
        ?.roundRect
        ?.topLeftCornerRadius
        ?.x
        ?.dp
}

private const val WALLET_PROBE_WIDTH = 320f
