package net.payrdr.mobile.payment.sdk.payment.ui.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bitmap of a country's flag emoji drawn by the platform, or null where Compose text renders flag
 * emoji itself. Needed on iOS: Compose there shows the regional-indicator pair as two empty boxes.
 */
internal expect fun flagBitmap(
    iso: String,
    sizePx: Int,
): ImageBitmap?

/** Country flag (emoji) of [size]. */
@Composable
internal fun FlagIcon(
    iso: String,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    val density = LocalDensity.current
    val px = with(density) { size.roundToPx() }
    val bitmap = remember(iso, px) { flagBitmap(iso, px) }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    } else {
        // Decorative like the bitmap: the country's name is always next to it, and TalkBack reads the emoji's own name.
        Text(
            flagEmoji(iso),
            style = TextStyle(fontSize = with(density) { (size * 0.85f).toSp() }),
            modifier = modifier.clearAndSetSemantics {},
        )
    }
}
