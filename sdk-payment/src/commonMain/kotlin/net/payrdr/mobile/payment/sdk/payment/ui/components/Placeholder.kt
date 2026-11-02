package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors

/**
 * Draw as the skeleton of the screen: components keep their exact size and layout, but text shows as a soft block of
 * its own size and logos, marks and buttons as blocks — so the skeleton is as tall as the screen it stands for.
 */
internal val LocalPlaceholder = staticCompositionLocalOf { false }

private val TextBlockShape = RoundedCornerShape(6.dp)

/** A block of the skeleton in place of a logo, mark or button. */
@Composable
internal fun PlaceholderBlock(
    modifier: Modifier,
    shape: Shape = TextBlockShape,
) {
    Box(modifier.background(LocalPaymentColors.current.surface, shape))
}

/** [color] for text, or transparent in a skeleton, where [placeholderText] draws the block instead. */
@Composable
@ReadOnlyComposable
internal fun textColor(color: Color): Color = if (LocalPlaceholder.current) Color.Transparent else color

/** In a skeleton, a block exactly the size of this text; nothing otherwise. */
@Composable
internal fun Modifier.placeholderText(): Modifier =
    if (LocalPlaceholder.current) background(LocalPaymentColors.current.surface, TextBlockShape) else this
