package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldBox
import net.payrdr.mobile.payment.sdk.payment.ui.components.FormField
import net.payrdr.mobile.payment.sdk.payment.ui.components.NativeTextField
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextFieldContentType
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneNumbers
import net.payrdr.mobile.payment.sdk.payment.ui.screens.message
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular
import net.payrdr.mobile.payment.sdk.payment.ui.util.FlagIcon
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.phone_code_cd
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.phone_code_choose
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource

/** Phone: a country-code button (flag, `+44`) and the national number, formatted as typed. */
@Composable
internal fun PhoneField(
    state: PayerFieldsState,
    spec: PayerFieldSpec,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    var focused by remember { mutableStateOf(false) }
    val error = state.visibleErrorOf(spec)?.let { stringResource(it.message()) }
    val editable = state.isEditable(spec)
    val country = state.phoneCountry
    val codeLabel = country?.let { ltr("+${it.dialCode}") } ?: stringResource(Res.string.phone_code_choose)
    val codeDescription = stringResource(Res.string.phone_code_cd, codeLabel)
    FormField(
        focused = focused,
        label = labelOf(spec),
        message = error,
        isError = error != null,
        modifier = Modifier.testTag("payer_${spec.field.apiName}"),
    ) {
        FieldBox(
            focused = focused,
            isError = error != null,
            readOnly = !editable,
            valid = editable && state.isVisiblyValid(spec),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = editable, role = Role.Button) { state.phonePickerOpen = true }
                        .semantics(mergeDescendants = true) { contentDescription = codeDescription }
                        .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (country != null) FlagIcon(country.iso)
                Text(
                    codeLabel,
                    style = typography.input.tabular().copy(color = if (country == null) colors.placeholder else colors.onBackground),
                )
                if (editable) {
                    Icon(
                        PaymentIcons.ChevronDown,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .height(22.dp)
                    .background(colors.outline),
            )
            NativeTextField(
                value = PhoneNumbers.format(state.phoneNational, country),
                onValueChange = state::onPhoneInput,
                placeholder = PhoneNumbers.placeholder(country),
                keyboardOptions = KeyboardOptions.Default,
                contentType = TextFieldContentType.PHONE,
                enabled = editable,
                onFocusChange = {
                    focused = it
                    state.onFocusChange(spec.field, it)
                },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = labelOf(spec),
                errorMessage = error,
            )
        }
    }
}
