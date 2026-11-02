package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.runtime.Composable

/**
 * True while the on-screen keyboard is shown or sliding in; flips as soon as it starts to hide, so
 * UI can animate together with the keyboard rather than after it.
 */
@Composable
internal expect fun rememberKeyboardOpen(): Boolean
