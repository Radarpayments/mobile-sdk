package net.payrdr.mobile.payment.sdk.payment.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTypography

/**
 * Lays out every [typography] style once in the background while the session loads.
 *
 * On iOS the first text in a style pays a one-off cost (font matching, fallback lookup, ICU) — 15–50 ms
 * per style. Without this it lands in the frame the form appears in, in the middle of the sheet animation.
 */
@Composable
internal fun WarmUpText(typography: PaymentTypography) {
    val density = LocalDensity.current
    LaunchedEffect(typography, density) {
        val measurer = textWarmUpMeasurer(density) ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            with(typography) { listOf(amount, caption, title, body, label, button, input, headline) }
                .forEach { measurer.measure(WARM_UP_SAMPLE, it) }
        }
    }
}

/**
 * Measurer for [WarmUpText], or null where the first text is cheap. It must not share text caches with
 * the UI: those are not meant for two threads at once.
 */
internal expect fun textWarmUpMeasurer(density: Density): TextMeasurer?

// Latin, Cyrillic, digits and the symbols amounts and placeholders use (grouping spaces, currencies, №, •).
private const val WARM_UP_SAMPLE = "Pay Оплатить 0123456789 1 249,90 € $ £ ₺ ₸ ₴ № •••• · —"
