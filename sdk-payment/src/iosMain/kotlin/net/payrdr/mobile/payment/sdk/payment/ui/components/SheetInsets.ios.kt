package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSValue
import platform.UIKit.CGRectValue
import platform.UIKit.UIKeyboardAnimationDurationUserInfoKey
import platform.UIKit.UIKeyboardFrameEndUserInfoKey
import platform.UIKit.UIKeyboardWillChangeFrameNotification
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIScreen

// Compose's ime inset on iOS changes only once the keyboard has finished moving, so a sheet padded with it stays
// up while the keyboard slides away and then drops in one frame. The keyboard's own notifications arrive as it
// starts to move and carry its duration: the sheet's bottom follows them instead.
internal actual fun Modifier.sheetSafeInsets(): Modifier =
    composed {
        val keyboard = remember { Animatable(0f) }
        var target by remember { mutableStateOf(KeyboardMove(0f, 0)) }
        DisposableEffect(Unit) {
            val center = NSNotificationCenter.defaultCenter
            val queue = NSOperationQueue.mainQueue
            val change =
                center.addObserverForName(
                    UIKeyboardWillChangeFrameNotification,
                    null,
                    queue,
                ) { note -> target = keyboardMove(note) }
            val hide =
                center.addObserverForName(UIKeyboardWillHideNotification, null, queue) { note ->
                    target =
                        keyboardMove(note).copy(height = 0f)
                }
            onDispose {
                center.removeObserver(change)
                center.removeObserver(hide)
            }
        }
        LaunchedEffect(target) { keyboard.animateTo(target.height, tween(target.durationMs, easing = KeyboardEasing)) }
        val density = LocalDensity.current
        val bars = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        val barsBottom = with(density) { bars.getBottom(density).toDp() }
        this
            .windowInsetsPadding(bars.only(WindowInsetsSides.Horizontal))
            // The keyboard covers the home indicator area, so it replaces that inset rather than adding to it. Without it,
            // the last button may reach into that area: the indicator keeps to its lowest part, and the sheet's own
            // padding still stands between them.
            .padding(bottom = maxOf(barsBottom - BottomInsetReach, 0.dp, keyboard.value.dp))
    }

/** Where the keyboard is going: its visible height in points and how long it takes to get there. */
private data class KeyboardMove(
    val height: Float,
    val durationMs: Int,
)

@OptIn(ExperimentalForeignApi::class)
private fun keyboardMove(note: NSNotification?): KeyboardMove {
    val info = note?.userInfo
    val duration = ((info?.get(UIKeyboardAnimationDurationUserInfoKey) as? NSNumber)?.doubleValue ?: DEFAULT_DURATION_S) * MS_PER_S
    val frame = info?.get(UIKeyboardFrameEndUserInfoKey) as? NSValue ?: return KeyboardMove(0f, duration.toInt())
    val screenHeight = UIScreen.mainScreen.bounds.useContents { size.height }
    val top = frame.CGRectValue().useContents { origin.y }
    return KeyboardMove((screenHeight - top).coerceAtLeast(0.0).toFloat(), duration.toInt())
}

/** How far into the home indicator area (34pt) the sheet's content may go: it ends ~13pt above the indicator. */
internal actual val BottomInsetReach: Dp = 20.dp

private const val DEFAULT_DURATION_S = 0.25
private const val MS_PER_S = 1000

/** Close to the curve the system keyboard moves with. */
private val KeyboardEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
