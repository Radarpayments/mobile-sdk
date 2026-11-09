package net.payrdr.mobile.payment.samples.android

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import net.payrdr.mobile.payment.samples.android.ui.components.BrandColors

private val LightScheme =
    lightColorScheme(
        primary = BrandColors.Indigo,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        secondary = BrandColors.Violet,
        background =
            androidx.compose.ui.graphics
                .Color(0xFFF5F7FB),
        surface = androidx.compose.ui.graphics.Color.White,
    )

/**
 * The sample's screens are drawn for a light background only (white cards, brand gradients), so
 * they stay light in the system dark mode. The checkout runs in its own activity and still follows
 * the system appearance.
 */
@Composable
fun SampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightScheme, content = content)
}
