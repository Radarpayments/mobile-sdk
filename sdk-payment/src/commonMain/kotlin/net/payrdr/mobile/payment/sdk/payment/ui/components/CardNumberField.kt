package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.RowScope
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
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_pan_label
import org.jetbrains.compose.resources.stringResource

/**
 * Card number input. Wraps [NativeTextField] to get native autofill, adds live grouping
 * (4-4-4-4, 4-6-5 for Amex) and caps the length per network. The network is shown on the card
 * preview, not in the field.
 *
 * @param value current raw digits (unformatted). Callers should persist *digits only*.
 * @param onValueChange fires with the NEW digits-only value on every user edit
 * @param enabled disables typing and greys out
 * @param errorMessage text shown under the field in the error colour; null = no error
 * @param valid the value is complete and correct: a green check at the right edge
 * @param labelActions optional actions on the right of the label, e.g. a Scan action
 */
@Composable
public fun CardNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    errorMessage: String? = null,
    labelActions: (@Composable RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
    valid: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val digits = CardFormatters.digitsOnly(value)
    val isError = errorMessage != null

    FormField(
        focused = focused,
        label = stringResource(Res.string.card_form_pan_label),
        message = errorMessage,
        isError = isError,
        labelActions = labelActions,
        modifier = modifier,
    ) {
        FieldBox(focused = focused, isError = isError, valid = valid) {
            NativeTextField(
                value = CardFormatters.formatPan(digits),
                onValueChange = { text ->
                    val next = CardFormatters.digitsOnly(text)
                    onValueChange(next.take(CardFormatters.maxPanLength(CardNetworkResolver.resolve(next))))
                },
                placeholder = "0000 0000 0000 0000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                contentType = TextFieldContentType.CREDIT_CARD_NUMBER,
                isError = isError,
                enabled = enabled,
                onFocusChange = { focused = it },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = stringResource(Res.string.card_form_pan_label),
                errorMessage = errorMessage,
            )
        }
    }
}
