package net.payrdr.mobile.payment.sdk.payment.scanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Guide frame in the 1.586:1 ID-1 aspect ratio, [FRAME_INSET] from the sides and centred, with the picture around it
 * dimmed so the card stands out. Green while a card is being read.
 */
@Composable
internal fun CardScannerOverlay(
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val inset = FRAME_INSET.toPx()
        val w = size.width - 2 * inset
        val h = w / CARD_ASPECT
        val topLeft = Offset(inset, (size.height - h) / 2f)
        val corner = CornerRadius(FRAME_CORNER.toPx())
        drawRect(Color.Black.copy(alpha = 0.5f))
        drawRoundRect(Color.Transparent, topLeft, Size(w, h), corner, blendMode = BlendMode.Clear)
        drawRoundRect(
            color = if (highlight) Color(0xFF34C759) else Color.White.copy(alpha = 0.9f),
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = corner,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

/** ID-1 card: 85.60 × 53.98 mm. */
private const val CARD_ASPECT = 1.586f

/** From the screen sides to the frame; the payment sheet's camera intro draws its frame at the same place. */
internal val FRAME_INSET = 28.dp

private val FRAME_CORNER = 14.dp
