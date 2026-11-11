package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.toPath
import kotlin.math.min

/**
 * Draws a [MonoLogo] in a single [color], scaled to fit and centred. Layers stack with their own opacity;
 * if the logo has knockout layers it is drawn in an offscreen layer so that each layer replaces the one
 * below (no darker overlaps) and knockouts become transparent.
 */
internal class MonoLogoPainter(
    private val logo: MonoLogo,
    private val color: Color,
) : Painter() {
    private val paths: List<Path> = pathsOf(logo)
    private val isolated = logo.layers.any { it.knockout }

    override val intrinsicSize: Size = Size(logo.width, logo.height)

    override fun DrawScope.onDraw() {
        val scale = min(size.width / logo.width, size.height / logo.height)
        val dx = (size.width - logo.width * scale) / 2
        val dy = (size.height - logo.height * scale) / 2
        drawIntoCanvas { canvas ->
            if (isolated) canvas.saveLayer(Rect(Offset.Zero, size), Paint())
            translate(dx, dy) {
                scale(scale, scale, pivot = Offset.Zero) {
                    translate(-logo.left, -logo.top) {
                        logo.layers.forEachIndexed { index, layer ->
                            drawPath(
                                path = paths[index],
                                color = if (layer.knockout) Color.Black else color.copy(alpha = color.alpha * layer.alpha),
                                blendMode =
                                    when {
                                        layer.knockout -> BlendMode.Clear
                                        isolated -> BlendMode.Src
                                        else -> BlendMode.SrcOver
                                    },
                            )
                        }
                    }
                }
            }
            if (isolated) canvas.restore()
        }
    }

    private companion object {
        /** Parsing the path data is the expensive part, so it happens once per logo. */
        private val cache = mutableMapOf<MonoLogo, List<Path>>()

        fun pathsOf(logo: MonoLogo): List<Path> = cache.getOrPut(logo) { logo.layers.map { addPathNodes(it.pathData).toPath() } }
    }
}
