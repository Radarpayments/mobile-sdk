package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable

/**
 * True when the user asked the system to reduce motion (Android: animator duration scale 0,
 * iOS: Reduce Motion). Decorative animations are switched off then.
 */
@Composable
internal expect fun rememberReduceMotion(): Boolean
