package net.payrdr.mobile.payment.sdk.payment.ui.util

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

// Own font resolver: the expensive part (system font lookup, ICU) is process-wide, so it still warms the UI.
internal actual fun textWarmUpMeasurer(density: Density): TextMeasurer? =
    TextMeasurer(createFontFamilyResolver(), density, LayoutDirection.Ltr, cacheSize = 0)
