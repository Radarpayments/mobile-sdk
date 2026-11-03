package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldBox
import net.payrdr.mobile.payment.sdk.payment.ui.components.FormField
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupFieldStatus
import net.payrdr.mobile.payment.sdk.payment.ui.components.NativeTextField
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextFieldContentType
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneNumbers
import net.payrdr.mobile.payment.sdk.payment.ui.screens.message
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_email_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_phone_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_address_line1_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_address_line2_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_address_line3_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_city_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_country_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_optional
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_postal_code_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_state_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_address
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_birthday
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_date_hint
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_first_name
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_number
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_last_name
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_middle_name
import org.jetbrains.compose.resources.stringResource

/** What a cell of a labeled group shows: its floating [label], and the [hint] under it while the field is empty. */
internal data class GroupFieldText(
    val label: String,
    val hint: String,
)

/** The field's [name], with an asterisk for a required field among optional ones; a date's [format] as the hint. */
internal fun groupFieldText(
    name: String,
    mandatory: Boolean,
    markMandatory: Boolean,
    format: String?,
): GroupFieldText = GroupFieldText(label = if (mandatory && markMandatory) "$name *" else name, hint = format.orEmpty())

@Composable
internal fun groupTextOf(
    spec: PayerFieldSpec,
    markMandatory: Boolean,
): GroupFieldText =
    groupFieldText(
        name = baseLabelOf(spec.field),
        mandatory = spec.mandatory,
        markMandatory = markMandatory,
        // A date names its format: the field takes digits only and puts the slashes in itself.
        format = if (TransferFields.kind(spec.field) == TransferFields.Kind.DATE) stringResource(Res.string.transfer_date_hint) else null,
    )

@Composable
internal fun PayerTextField(
    state: PayerFieldsState,
    spec: PayerFieldSpec,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val field = spec.field
    val error = state.visibleErrorOf(spec)?.let { stringResource(it.message()) }
    val editable = state.isEditable(spec)
    FormField(
        focused = focused,
        label = labelOf(spec),
        message = error,
        isError = error != null,
        modifier = modifier.testTag("payer_${field.apiName}"),
    ) {
        FieldBox(
            focused = focused,
            isError = error != null,
            readOnly = !editable,
            valid = editable && state.isVisiblyValid(spec),
        ) {
            NativeTextField(
                value = state.value(field),
                onValueChange = { state.onValueChange(field, it) },
                placeholder = placeholderOf(state, field),
                keyboardOptions = KeyboardOptions.Default,
                contentType = field.contentType,
                isError = error != null,
                enabled = editable,
                onFocusChange = {
                    focused = it
                    state.onFocusChange(field, it)
                },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                label = labelOf(spec),
                errorMessage = error,
            )
        }
    }
}

/** A grouped field's state, its error in words for the screen reader. */
@Composable
internal fun groupStatusOf(
    state: PayerFieldsState,
    spec: PayerFieldSpec,
): GroupFieldStatus {
    val error = state.visibleErrorOf(spec)
    return GroupFieldStatus(isError = error != null, message = error?.let { stringResource(it.message()) })
}

@Composable
internal fun labelOf(spec: PayerFieldSpec): String {
    val base = baseLabelOf(spec.field)
    return if (spec.mandatory) base else stringResource(Res.string.payer_optional, base)
}

@Composable
internal fun baseLabelOf(field: PayerField): String =
    stringResource(
        when (field) {
            PayerField.EMAIL -> Res.string.card_form_email_label
            PayerField.MOBILE_PHONE -> Res.string.card_form_phone_label
            PayerField.BILLING_COUNTRY -> Res.string.payer_country_label
            PayerField.BILLING_STATE -> Res.string.payer_state_label
            PayerField.BILLING_CITY -> Res.string.payer_city_label
            PayerField.BILLING_POSTAL_CODE -> Res.string.payer_postal_code_label
            PayerField.BILLING_ADDRESS_LINE1 -> Res.string.payer_address_line1_label
            PayerField.BILLING_ADDRESS_LINE2 -> Res.string.payer_address_line2_label
            PayerField.BILLING_ADDRESS_LINE3 -> Res.string.payer_address_line3_label
            PayerField.PAYER_FIRST_NAME, PayerField.RECIPIENT_FIRST_NAME -> Res.string.transfer_first_name
            PayerField.PAYER_MIDDLE_NAME, PayerField.RECIPIENT_MIDDLE_NAME -> Res.string.transfer_middle_name
            PayerField.PAYER_LAST_NAME, PayerField.RECIPIENT_LAST_NAME -> Res.string.transfer_last_name
            PayerField.PAYER_BIRTHDAY, PayerField.RECIPIENT_BIRTHDAY -> Res.string.transfer_birthday
            PayerField.PAYER_ACCOUNT, PayerField.RECIPIENT_ACCOUNT -> Res.string.transfer_account
            PayerField.PAYER_ACCOUNT_NUMBER_TYPE, PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE -> Res.string.transfer_account_type
            PayerField.PAYER_ID_TYPE, PayerField.RECIPIENT_ID_TYPE -> Res.string.transfer_id_type
            PayerField.PAYER_ID_NUMBER, PayerField.RECIPIENT_ID_NUMBER -> Res.string.transfer_id_number
            PayerField.RECIPIENT_COUNTRY -> Res.string.payer_country_label
            PayerField.RECIPIENT_STATE -> Res.string.payer_state_label
            PayerField.RECIPIENT_CITY -> Res.string.payer_city_label
            PayerField.RECIPIENT_ADDRESS_LINE1 -> Res.string.transfer_address
            PayerField.RECIPIENT_POSTAL_CODE -> Res.string.payer_postal_code_label
        },
    )

// Contact fields only: billing fields live in BillingGroup and use their names as hints.
@Composable
private fun placeholderOf(
    state: PayerFieldsState,
    field: PayerField,
): String =
    when (field) {
        PayerField.EMAIL -> "name@example.com"
        PayerField.MOBILE_PHONE -> PhoneNumbers.placeholder(state.phoneCountry)
        else -> baseLabelOf(field)
    }

internal val PayerField.isAddressLine: Boolean
    get() =
        this == PayerField.BILLING_ADDRESS_LINE1 ||
            this == PayerField.BILLING_ADDRESS_LINE2 ||
            this == PayerField.BILLING_ADDRESS_LINE3

internal val PayerField.contentType: TextFieldContentType
    get() =
        when (this) {
            PayerField.EMAIL -> TextFieldContentType.EMAIL
            PayerField.MOBILE_PHONE -> TextFieldContentType.PHONE
            PayerField.BILLING_CITY, PayerField.RECIPIENT_CITY -> TextFieldContentType.CITY
            PayerField.BILLING_STATE, PayerField.RECIPIENT_STATE -> TextFieldContentType.STATE
            PayerField.BILLING_POSTAL_CODE, PayerField.RECIPIENT_POSTAL_CODE -> TextFieldContentType.POSTAL_CODE
            PayerField.BILLING_ADDRESS_LINE1,
            PayerField.BILLING_ADDRESS_LINE2,
            PayerField.BILLING_ADDRESS_LINE3,
            PayerField.RECIPIENT_ADDRESS_LINE1,
            -> TextFieldContentType.ADDRESS_LINE
            else -> TextFieldContentType.NONE
        }
