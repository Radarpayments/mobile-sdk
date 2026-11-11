package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True for a [NativeTextField] that should take focus as soon as it appears (e.g. an address line the user has
 * just added). Read once, when the field enters the composition.
 */
internal val LocalNativeFieldAutoFocus = staticCompositionLocalOf { false }

/**
 * True once the step that holds a field has finished sliding in. A field that takes focus as it appears waits
 * for it: the keyboard opening during the slide stalls the frames, and the slide would be lost.
 */
internal val LocalStepSettled = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(true) }
