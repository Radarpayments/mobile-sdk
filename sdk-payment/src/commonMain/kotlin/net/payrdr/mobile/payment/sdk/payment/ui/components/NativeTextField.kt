package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Platform-hinted content type for [NativeTextField]. Maps to the autofill `ContentType` on Android and
 * `UITextContentType` on iOS so that Keychain / Google Autofill / card scanners can populate
 * the field automatically. It also picks the keyboard, tabular digits and CVV masking.
 */
internal enum class TextFieldContentType {
    CREDIT_CARD_NUMBER,
    CREDIT_CARD_EXPIRATION,
    CREDIT_CARD_SECURITY_CODE,
    CREDIT_CARD_HOLDER,
    EMAIL,
    PHONE,

    /** Street address line; Latin keyboard. */
    ADDRESS_LINE,

    /** City; Latin keyboard. */
    CITY,

    /** State / region; Latin keyboard. */
    STATE,

    /** Postal / ZIP code; Latin keyboard. */
    POSTAL_CODE,
    NONE,
}

/**
 * Typed in Latin script (card data, email, phone, address): the text runs left to right even in an Arabic layout —
 * otherwise the groups of a card or phone number come out in reverse order — and still sits at the layout's start.
 */
internal val TextFieldContentType.isLatin: Boolean
    get() = this != TextFieldContentType.STATE && this != TextFieldContentType.NONE

/** Digits-only content: rendered with tabular figures and a number pad. */
internal val TextFieldContentType.isNumeric: Boolean
    get() =
        this == TextFieldContentType.CREDIT_CARD_NUMBER ||
            this == TextFieldContentType.CREDIT_CARD_EXPIRATION ||
            this == TextFieldContentType.CREDIT_CARD_SECURITY_CODE

/**
 * Platform-native text field. On Android this is a Compose `BasicTextField` typed for autofill; on iOS the card
 * fields are a `UIKitView(UITextField)` with the right `textContentType`, and the rest the same Compose field with
 * that type passed to the keyboard.
 *
 * Both actuals are chromeless — no border, no fill. Frame, focus ring and error colouring are drawn
 * by the caller (see `FieldBox`), which learns about focus through [onFocusChange].
 * [TextFieldContentType.CREDIT_CARD_SECURITY_CODE] input is masked.
 *
 * @param placeholder placeholder text shown when [value] is empty
 * @param keyboardOptions keyboard type / IME action hints
 * @param contentType autofill hint — maps to the autofill ContentType (Android) / UITextContentType (iOS)
 * @param isError the field currently shows an error (the frame draws it)
 * @param enabled when false, the field is read-only and greyed out
 * @param modifier size / padding of the underlying control
 * @param onFocusChange fires with true when the field gains input focus and false when it loses it
 * @param label the field's name, read by screen readers with its value; null reads [placeholder] instead, which is
 *   gone once the field is filled
 * @param errorMessage what is wrong with the value, read by screen readers with the field; null when nothing is
 */
@Composable
internal expect fun NativeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    contentType: TextFieldContentType,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onFocusChange: (Boolean) -> Unit = {},
    label: String? = null,
    errorMessage: String? = null,
)

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
