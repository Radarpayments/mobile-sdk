package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun NativeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    contentType: TextFieldContentType,
    isError: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onFocusChange: (Boolean) -> Unit,
    label: String?,
    errorMessage: String?,
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

@Composable
internal actual fun rememberPlatformKeyboard(
    options: KeyboardOptions,
    contentType: TextFieldContentType,
): PlatformKeyboard = PlatformKeyboard(options, KeyboardActions.Default)
