package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable

/**
 * Closes the keyboard of the field that has it. A native iOS field sits outside Compose focus, so clearing
 * the focus alone would leave its keyboard up — over a camera opened on top of the form, say.
 */
@Composable
internal expect fun rememberKeyboardDismiss(): () -> Unit
