package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first

/**
 * Tells the screen reader that a new step is on screen once it has slid in, so that it reads the step from its top
 * instead of staying on a button that is gone. On Android the step's pane title does it (see [StepHeader]); iOS
 * needs the notification.
 */
@Composable
internal fun AnnounceScreenChange() {
    val settled = LocalStepSettled.current
    LaunchedEffect(Unit) {
        snapshotFlow { settled.value }.first { it }
        withFrameNanos { }
        postScreenChanged()
    }
}

/** UIKit's "screen changed" notification on iOS; nothing on Android. */
internal expect fun postScreenChanged()
