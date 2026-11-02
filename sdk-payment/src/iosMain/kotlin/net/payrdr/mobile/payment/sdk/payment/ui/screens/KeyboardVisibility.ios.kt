package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSValue
import platform.UIKit.CGRectValue
import platform.UIKit.UIKeyboardFrameEndUserInfoKey
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification

/** Below this the "keyboard" is only the accessory bar shown over a hardware keyboard. */
private const val MIN_KEYBOARD_HEIGHT = 120.0

// Compose's ime inset reaches 0 only after the hide animation ends; the will-show/hide
// notifications arrive as the keyboard starts moving.
@Composable
internal actual fun rememberKeyboardOpen(): Boolean {
    var open by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val queue = NSOperationQueue.mainQueue
        val show =
            center.addObserverForName(UIKeyboardWillShowNotification, null, queue) { note ->
                open = keyboardHeight(note) >= MIN_KEYBOARD_HEIGHT
            }
        val hide = center.addObserverForName(UIKeyboardWillHideNotification, null, queue) { open = false }
        onDispose {
            center.removeObserver(show)
            center.removeObserver(hide)
        }
    }
    return open
}

@OptIn(ExperimentalForeignApi::class)
private fun keyboardHeight(note: NSNotification?): Double {
    val frame = note?.userInfo?.get(UIKeyboardFrameEndUserInfoKey) as? NSValue ?: return 0.0
    return frame.CGRectValue().useContents { size.height }
}
