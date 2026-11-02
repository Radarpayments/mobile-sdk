package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction
import platform.darwin.NSObject

/**
 * The keyboard the UITextFields had, for the fields Compose draws: no autocorrection, postal codes in capitals, a
 * return key labelled "Done" that leaves the field, and a "Done" bar over the number and phone pads, which have no
 * return key.
 *
 * The autofill type and the bar only reach UIKit through `PlatformImeOptions`, still experimental in Compose
 * Multiplatform 1.10: JetBrains documents it as the way to autofill a Compose field on iOS. Keep its use here.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun rememberPlatformKeyboard(
    options: KeyboardOptions,
    contentType: TextFieldContentType,
): PlatformKeyboard {
    val focusManager = rememberUpdatedState(LocalFocusManager.current)
    val done = remember { KeyboardDoneTarget { focusManager.value.clearFocus() } }
    return remember(options, contentType) {
        val noReturnKey =
            options.keyboardType == KeyboardType.Number ||
                options.keyboardType == KeyboardType.NumberPassword ||
                options.keyboardType == KeyboardType.Phone
        val textContentType = contentTypeFor(contentType)
        PlatformKeyboard(
            options =
                options.copy(
                    autoCorrectEnabled = false,
                    capitalization =
                        if (contentType == TextFieldContentType.POSTAL_CODE) KeyboardCapitalization.Characters else options.capitalization,
                    imeAction = if (options.imeAction == ImeAction.Default) ImeAction.Done else options.imeAction,
                    platformImeOptions =
                        PlatformImeOptions {
                            if (textContentType != null) textContentType(textContentType)
                            if (noReturnKey) inputAccessoryView(doneToolbar(done))
                        },
                ),
            // Compose only hides the keyboard on "Done"; a UITextField leaves the field, and so does this.
            actions = KeyboardActions(onDone = { focusManager.value.clearFocus() }),
        )
    }
}

/** Target of the "Done" bar's button: UIKit calls `onDone` on an Objective-C object, not on a Kotlin lambda. */
private class KeyboardDoneTarget(
    private val action: () -> Unit,
) : NSObject() {
    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun onDone() {
        action()
    }
}
