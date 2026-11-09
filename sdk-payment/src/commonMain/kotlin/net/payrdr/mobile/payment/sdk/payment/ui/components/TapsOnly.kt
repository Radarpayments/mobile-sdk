package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Takes taps without making the area a button for screen readers, as `clickable` would: a scrim, a sheet's handle, the
 * sheet swallowing taps that would reach the scrim. A screen reader reaches the same through the Close button and the
 * sheet's dismiss action, and a `clickable` sheet would read all its text as one nameless button.
 */
@Composable
internal fun Modifier.tapsOnly(onTap: () -> Unit = {}): Modifier {
    val latest by rememberUpdatedState(onTap)
    return pointerInput(Unit) { detectTapGestures { latest() } }
}
