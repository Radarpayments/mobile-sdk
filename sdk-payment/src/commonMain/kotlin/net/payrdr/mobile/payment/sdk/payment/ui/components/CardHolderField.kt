package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import net.payrdr.mobile.payment.sdk.core.validation.CardHolderValidator
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_holder_label
import org.jetbrains.compose.resources.stringResource

/**
 * Cardholder name input. Applies uppercase transform on every keystroke (embossed cards print
 * holders in uppercase). Not part of the default card form; kept for merchants who build their own.
 */
@Composable
public fun CardHolderField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val validation = remember(value) { CardHolderValidator().validate(value.trim()) }
    val isError = value.isNotBlank() && !validation.isValid

    FormField(
        focused = focused,
        label = stringResource(Res.string.card_form_holder_label),
        message = null,
        isError = isError,
        modifier = modifier,
    ) {
        FieldBox(focused = focused, isError = isError) {
            NativeTextField(
                value = value,
                onValueChange = { newText -> onValueChange(newText.uppercase()) },
                placeholder = "CARD HOLDER",
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters,
                    ),
                contentType = TextFieldContentType.CREDIT_CARD_HOLDER,
                isError = isError,
                enabled = enabled,
                onFocusChange = { focused = it },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = stringResource(Res.string.card_form_holder_label),
            )
        }
    }
}
