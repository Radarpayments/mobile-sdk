package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
internal actual fun rememberKeyboardDismiss(): () -> Unit {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return remember(focus, keyboard) {
        {
            focus.clearFocus(force = true)
            keyboard?.hide()
        }
    }
}
