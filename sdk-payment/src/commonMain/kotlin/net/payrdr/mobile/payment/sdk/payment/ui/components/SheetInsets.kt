package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Keeps the sheet's content clear of the system bars and the keyboard at the bottom and sides, moving together
 * with the keyboard as it slides in and out, so the sheet rides on the keys rather than jumping after them.
 */
internal expect fun Modifier.sheetSafeInsets(): Modifier

/**
 * How far into the bottom system inset the last button, card or plate of a screen may go. The sheet's buttons and the
 * camera screen's bottom card end on the same line: the system inset minus this, plus their own 12dp.
 */
internal expect val BottomInsetReach: Dp
