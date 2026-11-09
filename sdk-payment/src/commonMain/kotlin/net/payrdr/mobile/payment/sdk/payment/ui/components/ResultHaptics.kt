package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable

/**
 * The system's own "done" / "failed" tap, as the platform plays it for its own confirmations (iOS notification
 * feedback, Android CONFIRM / REJECT). Played whatever the reduced-motion setting.
 */
@Composable
internal expect fun rememberResultHaptics(): (success: Boolean) -> Unit
