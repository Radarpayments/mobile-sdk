package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Android animates the ime inset with the keyboard.
internal actual fun Modifier.sheetSafeInsets(): Modifier =
    composed { windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)) }

// An opaque navigation bar can sit there: nothing goes into it.
internal actual val BottomInsetReach: Dp = 0.dp
