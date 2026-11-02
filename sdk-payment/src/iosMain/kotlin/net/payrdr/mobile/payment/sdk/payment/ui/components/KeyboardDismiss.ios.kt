package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberKeyboardDismiss(): () -> Unit {
    val focus = LocalFocusManager.current
    return remember(focus) {
        {
            focus.clearFocus(force = true)
            // Sent to the first responder, whichever native field it is.
            UIApplication.sharedApplication.sendAction(
                NSSelectorFromString("resignFirstResponder"),
                to = null,
                from = null,
                forEvent = null,
            )
        }
    }
}
