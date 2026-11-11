package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.first
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular

/**
 * The text field behind every [NativeTextField].
 *
 * @param floatingLabel the field's name over its input, see [GroupFieldLabel]; [placeholder] then shows under it
 * @param label the field's name for screen readers when it has no [floatingLabel] (shown as a line over the field)
 * @param errorMessage what is wrong with the value, read by screen readers with the field; null when nothing is
 */
@Composable
internal fun ComposeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    contentType: TextFieldContentType,
    enabled: Boolean,
    modifier: Modifier,
    onFocusChange: (Boolean) -> Unit,
    floatingLabel: FloatingLabel? = null,
    label: String? = null,
    errorMessage: String? = null,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    val latestOnFocus = rememberUpdatedState(onFocusChange)
    val autoFocus = LocalNativeFieldAutoFocus.current
    val settled = LocalStepSettled.current
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (autoFocus) {
            snapshotFlow { settled.value }.first { it }
            focusRequester.requestFocus()
        }
    }

    val numeric = contentType.isNumeric || contentType == TextFieldContentType.PHONE
    val baseStyle: TextStyle = if (numeric) typography.input.tabular() else typography.input
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val textStyle =
        if (contentType.isLatin) {
            baseStyle.copy(
                color = colors.onBackground,
                textDirection = TextDirection.Ltr,
                textAlign = if (rtl) TextAlign.Right else TextAlign.Left,
            )
        } else {
            baseStyle.copy(color = colors.onBackground)
        }
    val secure = contentType == TextFieldContentType.CREDIT_CARD_SECURITY_CODE

    // Callers reformat what the user typed ("41111" → "4111 1"). Keep the caret after the same
    // number of digits instead of letting it land in front of an inserted separator.
    var state by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    var digitsBeforeCaret by remember { mutableIntStateOf(-1) }
    val shown =
        if (state.text == value) {
            state
        } else {
            val caret = if (numeric && digitsBeforeCaret >= 0) caretAfterDigits(value, digitsBeforeCaret) else value.length
            TextFieldValue(value, TextRange(caret))
        }
    SideEffect { if (state != shown) state = shown }

    BasicTextField(
        value = shown,
        onValueChange = { next ->
            digitsBeforeCaret = next.text.take(next.selection.end).count { it.isDigit() }
            state = next
            if (next.text != shown.text) onValueChange(next.text)
        },
        singleLine = true,
        enabled = enabled,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = keyboardOptions.forContent(contentType),
        visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
        modifier =
            modifier
                .autofillContent(contentType)
                .semantics { if (errorMessage != null) error(errorMessage) }
                .focusRequester(focusRequester)
                .onFocusChanged {
                    focused = it.isFocused
                    latestOnFocus.value(it.isFocused)
                },
        decorationBox = { innerTextField ->
            val raised = groupLabelRaised(focused, value)
            // Passes the field's height on, so that the text stays in the middle of a field without a floating label.
            Box(propagateMinConstraints = true) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                        if (floatingLabel == null) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier.fillMaxSize().padding(top = LABELED_INPUT_TOP)
                        },
                ) {
                    // Under a floating label the hint (a date's format) shows only once the label has risen.
                    if (value.isEmpty() && (floatingLabel == null || raised)) {
                        // A worded hint reads in its own direction; a sample number ("201 555 0123") left to right.
                        Text(
                            text = placeholder,
                            style = textStyle.copy(color = colors.placeholder, textDirection = TextDirection.ContentOrLtr),
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    innerTextField()
                }
                if (floatingLabel != null) {
                    GroupFieldLabel(floatingLabel.text, raised, groupLabelTone(raised, focused, floatingLabel.isError))
                } else if (label != null && !(value.isEmpty() && placeholder == label)) {
                    // The name the screen reader reads with the value: the hint is gone once the field is filled, and the
                    // name shown over the field is a separate line it does not tie to it. A hint that is the name says it.
                    Box(Modifier.semantics { text = AnnotatedString(label) })
                }
            }
        },
    )
}

private fun caretAfterDigits(
    text: String,
    digits: Int,
): Int {
    if (digits <= 0) return 0
    var seen = 0
    text.forEachIndexed { index, char ->
        if (char.isDigit() && ++seen == digits) return index + 1
    }
    return text.length
}

private fun KeyboardOptions.forContent(contentType: TextFieldContentType): KeyboardOptions =
    when (contentType) {
        TextFieldContentType.EMAIL -> copy(keyboardType = KeyboardType.Email)
        TextFieldContentType.PHONE -> copy(keyboardType = KeyboardType.Phone)
        TextFieldContentType.ADDRESS_LINE,
        TextFieldContentType.CITY,
        TextFieldContentType.STATE,
        TextFieldContentType.POSTAL_CODE,
        -> copy(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Words)
        else -> this
    }

/**
 * Tells autofill what the field holds, through the field's semantics: since Compose UI 1.8 the framework builds the
 * autofill structure from semantics, so a masked field without a type reads as a password — the CVV next to an e-mail
 * made Google Password Manager offer to save a "password" when the checkout closed. With a type the CVV is card data,
 * filled from saved cards and never stored as a password.
 */
private fun Modifier.autofillContent(contentType: TextFieldContentType): Modifier {
    val type =
        when (contentType) {
            TextFieldContentType.CREDIT_CARD_NUMBER -> ContentType.CreditCardNumber
            TextFieldContentType.CREDIT_CARD_EXPIRATION -> ContentType.CreditCardExpirationDate
            TextFieldContentType.CREDIT_CARD_SECURITY_CODE -> ContentType.CreditCardSecurityCode
            TextFieldContentType.CREDIT_CARD_HOLDER -> ContentType.PersonFullName
            TextFieldContentType.EMAIL -> ContentType.EmailAddress
            TextFieldContentType.PHONE -> ContentType.PhoneNumber
            TextFieldContentType.ADDRESS_LINE -> ContentType.AddressStreet
            TextFieldContentType.CITY -> ContentType.AddressLocality
            TextFieldContentType.STATE -> ContentType.AddressRegion
            TextFieldContentType.POSTAL_CODE -> ContentType.PostalCode
            TextFieldContentType.NONE -> return this
        }
    return semantics { this.contentType = type }
}
