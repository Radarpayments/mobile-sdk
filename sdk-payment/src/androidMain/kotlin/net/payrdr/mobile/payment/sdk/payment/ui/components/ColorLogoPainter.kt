package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.toPath
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a [ColorLogo] in its brand colours, scaled to fit and centred. With [dark] the logo's
 * [ColorLogo.onDark] swaps apply; a specified [tint] paints every layer in that one colour instead.
 */
internal class ColorLogoPainter(
    private val logo: ColorLogo,
    private val dark: Boolean,
    private val tint: Color = Color.Unspecified,
) : Painter() {
    private val shapes: Shapes = shapesOf(logo)

    override val intrinsicSize: Size = shapes.bounds.size

    override fun DrawScope.onDraw() {
        val bounds = shapes.bounds
        val scale = min(size.width / bounds.width, size.height / bounds.height)
        val dx = (size.width - bounds.width * scale) / 2
        val dy = (size.height - bounds.height * scale) / 2
        translate(dx, dy) {
            scale(scale, scale, pivot = Offset.Zero) {
                translate(-bounds.left, -bounds.top) {
                    logo.layers.forEachIndexed { index, layer ->
                        val gradient = layer.gradient
                        when {
                            tint != Color.Unspecified -> drawPath(shapes.paths[index], tint)
                            gradient != null -> drawPath(shapes.paths[index], brushOf(gradient))
                            else -> drawPath(shapes.paths[index], colorOf(layer.color))
                        }
                    }
                }
            }
        }
    }

    private fun colorOf(argb: Long): Color = Color((if (dark) logo.onDark[argb] ?: argb else argb).toInt())

    private fun brushOf(gradient: LogoGradient): Brush =
        Brush.linearGradient(
            colorStops = gradient.stops.map { (offset, argb) -> offset to colorOf(argb) }.toTypedArray(),
            start = Offset(gradient.startX, gradient.startY),
            end = Offset(gradient.endX, gradient.endY),
        )

    /** Parsed layers of a logo and the box around all of them. */
    private class Shapes(
        val paths: List<Path>,
        val bounds: Rect,
    )

    private companion object {
        /** Parsing the path data is the expensive part, so it happens once per logo. */
        private val cache = mutableMapOf<ColorLogo, Shapes>()

        fun shapesOf(logo: ColorLogo): Shapes =
            cache.getOrPut(logo) {
                val paths = logo.layers.map { addPathNodes(it.pathData).toPath() }
                val bounds =
                    paths.map { it.getBounds() }.reduce { a, b ->
                        Rect(min(a.left, b.left), min(a.top, b.top), max(a.right, b.right), max(a.bottom, b.bottom))
                    }
                Shapes(paths, bounds)
            }
    }
}
