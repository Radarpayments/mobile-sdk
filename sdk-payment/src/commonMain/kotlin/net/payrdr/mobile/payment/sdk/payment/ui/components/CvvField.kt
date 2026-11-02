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
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetwork
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_cvv_label
import org.jetbrains.compose.resources.stringResource

/**
 * CVV input. Length depends on the card network (4 on the front for Amex, 3 on the back
 * otherwise); input is masked on both platforms.
 *
 * @param label field label; defaults to "CVC/CVV"
 * @param errorMessage text shown under the field in the error colour; null = nothing under the field
 * @param valid the value is complete and correct: a green check at the right edge
 */
@Composable
public fun CvvField(
    value: String,
    onValueChange: (String) -> Unit,
    network: CardNetwork = CardNetwork.UNKNOWN,
    enabled: Boolean = true,
    label: String = stringResource(Res.string.card_form_cvv_label),
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    valid: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val maxLen = CardFormatters.cvvLength(network)
    val digits = CardFormatters.digitsOnly(value).take(maxLen)
    val isError = errorMessage != null

    FormField(
        focused = focused,
        label = label,
        message = errorMessage,
        isError = isError,
        modifier = modifier,
    ) {
        FieldBox(focused = focused, isError = isError, valid = valid) {
            NativeTextField(
                value = digits,
                onValueChange = { text -> onValueChange(CardFormatters.digitsOnly(text).take(maxLen)) },
                placeholder = "•".repeat(maxLen),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                contentType = TextFieldContentType.CREDIT_CARD_SECURITY_CODE,
                isError = isError,
                enabled = enabled,
                onFocusChange = { focused = it },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = label,
                errorMessage = errorMessage,
            )
        }
    }
}
