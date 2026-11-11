package net.payrdr.mobile.payment.sdk.payment.ui.util

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Country flag (emoji) of [size]. */
@Composable
internal fun FlagIcon(
    iso: String,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    val density = LocalDensity.current
    // Decorative: the country's name is always next to it, and TalkBack would read the emoji's own name.
    Text(
        flagEmoji(iso),
        style = TextStyle(fontSize = with(density) { (size * 0.85f).toSp() }),
        modifier = modifier.clearAndSetSemantics {},
    )
}
