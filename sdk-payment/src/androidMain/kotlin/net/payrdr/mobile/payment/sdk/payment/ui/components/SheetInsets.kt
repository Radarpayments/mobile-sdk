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

/**
 * Keeps the sheet's content clear of the system bars and the keyboard at the bottom and sides. Android animates the
 * ime inset with the keyboard, so the sheet rides on the keys rather than jumping after them.
 */
internal fun Modifier.sheetSafeInsets(): Modifier =
    composed { windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)) }

/**
 * How far into the bottom system inset the last button, card or plate of a screen may go: nothing, as an opaque
 * navigation bar can sit there. The sheet's buttons and the camera screen's bottom card end on the same line: the
 * system inset minus this, plus their own 12dp.
 */
internal val BottomInsetReach: Dp = 0.dp
