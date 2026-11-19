package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType

/**
 * How an input looks and types: the [placeholder] shown in grey while it is empty, one line or several, a secret, and
 * whether the keyboard may correct what is typed.
 */
internal data class InputLook(
    val placeholder: String = "",
    val singleLine: Boolean = true,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val style: TextStyle = QaType.input,
    val hidden: Boolean = false,
    val autoCorrect: Boolean = true,
)
