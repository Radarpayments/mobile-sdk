package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_expiry_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_expiry_placeholder
import org.jetbrains.compose.resources.stringResource

/**
 * MM/YY expiry input. Keeps [value] as raw digits ("1225"); displays as "MM/YY". A first digit
 * above 1 is completed to a month ("5" → "05").
 *
 * @param errorMessage text shown under the field in the error colour; null = no error
 * @param valid the value is complete and correct: a green check at the right edge
 */
@Composable
public fun ExpiryField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    valid: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val digits = CardFormatters.digitsOnly(value).take(4)
    val isError = errorMessage != null

    FormField(
        focused = focused,
        label = stringResource(Res.string.card_form_expiry_label),
        message = errorMessage,
        isError = isError,
        modifier = modifier,
    ) {
        FieldBox(focused = focused, isError = isError, valid = valid) {
            NativeTextField(
                value = CardFormatters.formatExpiry(digits),
                onValueChange = { text ->
                    var next = CardFormatters.digitsOnly(text).take(4)
                    if (next.length == 1 && next[0] > '1') next = "0$next"
                    onValueChange(next)
                },
                placeholder = stringResource(Res.string.card_form_expiry_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                contentType = TextFieldContentType.CREDIT_CARD_EXPIRATION,
                isError = isError,
                enabled = enabled,
                onFocusChange = { focused = it },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = stringResource(Res.string.card_form_expiry_label),
                errorMessage = errorMessage,
            )
        }
    }
}
