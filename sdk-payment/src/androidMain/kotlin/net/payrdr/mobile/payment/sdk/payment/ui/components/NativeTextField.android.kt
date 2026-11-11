package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The checkout's text field: a Compose `BasicTextField` typed for autofill (see [ComposeTextField]).
 *
 * It is chromeless — no border, no fill. Frame, focus ring and error colouring are drawn
 * by the caller (see `FieldBox`), which learns about focus through [onFocusChange].
 * [TextFieldContentType.CREDIT_CARD_SECURITY_CODE] input is masked.
 *
 * @param placeholder placeholder text shown when [value] is empty
 * @param keyboardOptions keyboard type / IME action hints
 * @param contentType autofill hint — maps to the autofill ContentType
 * @param enabled when false, the field is read-only and greyed out
 * @param modifier size / padding of the underlying control
 * @param onFocusChange fires with true when the field gains input focus and false when it loses it
 * @param label the field's name, read by screen readers with its value; null reads [placeholder] instead, which is
 *   gone once the field is filled
 * @param errorMessage what is wrong with the value, read by screen readers with the field; null when nothing is
 */
@Composable
internal fun NativeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    contentType: TextFieldContentType,
    enabled: Boolean,
    modifier: Modifier,
    onFocusChange: (Boolean) -> Unit = {},
    label: String? = null,
    errorMessage: String? = null,
) {
    ComposeTextField(
        value,
        onValueChange,
        placeholder,
        keyboardOptions,
        contentType,
        enabled,
        modifier,
        onFocusChange,
        label = label,
        errorMessage = errorMessage,
    )
}
