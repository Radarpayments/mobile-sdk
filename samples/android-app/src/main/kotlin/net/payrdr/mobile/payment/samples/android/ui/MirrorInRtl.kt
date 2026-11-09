package net.payrdr.mobile.payment.samples.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Flips a directional icon (back, chevron) in a right-to-left layout: Tabler's icons are not auto-mirrored. */
@Composable
internal fun Modifier.mirrorInRtl(): Modifier = if (LocalLayoutDirection.current == LayoutDirection.Rtl) scale(-1f, 1f) else this
