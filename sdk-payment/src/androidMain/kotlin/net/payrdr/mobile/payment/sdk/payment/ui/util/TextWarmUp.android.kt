package net.payrdr.mobile.payment.sdk.payment.ui.util

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density

// System fonts come preloaded with the app process: nothing to warm up.
internal actual fun textWarmUpMeasurer(density: Density): TextMeasurer? = null
