package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.flow.Countries
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.payer.BillingView
import net.payrdr.mobile.payment.sdk.payment.ui.payer.PayerFieldsState
import net.payrdr.mobile.payment.sdk.payment.ui.payer.TransferFields
import net.payrdr.mobile.payment.sdk.payment.ui.payer.contentType
import net.payrdr.mobile.payment.sdk.payment.ui.payer.isAddressLine
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneCountries
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneNumbers
import net.payrdr.mobile.payment.sdk.payment.ui.util.countryDisplayName
import net.payrdr.mobile.payment.sdk.payment.ui.util.deviceRegion
import net.payrdr.mobile.payment.sdk.payment.ui.util.flagEmoji
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr

/**
 * The payer fields the gateway asks for with a card, in the blocks the form shows them in.
 *
 * @property billingCollapsible every billing field is optional, so the block starts folded.
 */
@PayrdrUiApi
public class PayerSections internal constructor(
    public val all: List<PayerFieldSpec>,
    public val contact: List<PayerFieldSpec>,
    public val billing: List<PayerFieldSpec>,
    public val sender: List<PayerFieldSpec>,
    public val recipient: List<PayerFieldSpec>,
    public val canAddAddressLine: Boolean,
    public val billingCollapsible: Boolean,
)

/** A country in a picker. */
@PayrdrUiApi
public class CountryOptionSpec internal constructor(
    public val code: String,
    public val name: String,
    public val englishName: String,
    public val dialCode: String?,
) {
    /** Whether a picker search for [query] keeps this country: by name, code or, with digits, dial code. */
    public fun matches(query: String): Boolean {
        if (query.isEmpty()) return true
        val digits = query.removePrefix("+")
        return name.contains(query, ignoreCase = true) ||
            englishName.contains(query, ignoreCase = true) ||
            code.equals(query, ignoreCase = true) ||
            (dialCode != null && digits.isNotEmpty() && digits.all(Char::isDigit) && dialCode.startsWith(digits))
    }
}

/** A line of the folded payer data: [icon] is `mail`, `phone` or `pin`. */
@PayrdrUiApi
public class PayerSummaryLine internal constructor(
    public val icon: String,
    public val text: String,
)

/**
 * The payer fields of one card step for the SwiftUI form: the state, rules and wording of the Compose form. It does not
 * notify of changes, the caller redraws after each call that changes it.
 */
@PayrdrUiApi
@Suppress("TooManyFunctions")
public class PayerForm internal constructor(
    request: PayerDataRequest,
    prefill: Map<PayerField, String>,
    private val strings: PaymentStrings,
) {
    private val state = PayerFieldsState(request, prefill, deviceRegion())

    /** The fields asked for a card of [paymentSystems], split into the form's blocks. */
    public fun sections(paymentSystems: List<String>): PayerSections {
        val specs = state.specsFor(paymentSystems)
        val contact = specs.filter { it.field == PayerField.EMAIL || it.field == PayerField.MOBILE_PHONE }
        val sender = specs.filter { it.field in TransferFields.SENDER }
        val recipient = specs.filter { it.field in TransferFields.RECIPIENT }
        val billing = specs - (contact + sender + recipient).toSet()
        return PayerSections(
            all = specs,
            contact = contact,
            billing = billing,
            sender = sender,
            recipient = recipient,
            canAddAddressLine = state.canAddAddressLine(paymentSystems),
            billingCollapsible = billing.none { it.mandatory },
        )
    }

    /** What [field] holds now. */
    public fun value(field: PayerField): String = state.value(field)

    /** The payer typed [text] into [field]. */
    public fun setValue(
        field: PayerField,
        text: String,
    ): Unit = state.onValueChange(field, text)

    /** [field] gained or lost focus; leaving it lets its error show. */
    public fun focusChanged(
        field: PayerField,
        focused: Boolean,
    ): Unit = state.onFocusChange(field, focused)

    /** False only when the gateway forbids editing the field and it has a value to show. */
    public fun isEditable(spec: PayerFieldSpec): Boolean = state.isEditable(spec)

    /** The payer has left the field filled in and correct, so it gets a check. */
    public fun isVisiblyValid(spec: PayerFieldSpec): Boolean = state.isVisiblyValid(spec)

    /** What is wrong with a field the payer has left, in words; null when nothing is. */
    public fun visibleError(spec: PayerFieldSpec): String? = state.visibleErrorOf(spec)?.let { strings.text(it.messageKey) }

    /** Whether any of [specs] is invalid now, shown or not. */
    public fun hasError(specs: List<PayerFieldSpec>): Boolean = specs.any { state.errorOf(it) != null }

    /** Marks every one of [specs] touched; true when all of them are valid. */
    public fun validateAll(specs: List<PayerFieldSpec>): Boolean = state.validateAll(specs)

    /** The non-blank values of [specs], to send with the payment. */
    public fun submitValues(specs: List<PayerFieldSpec>): Map<PayerField, String> = state.submitValues(specs)

    /** `add`, `fields` or `summary`: what an all-optional billing block shows. */
    public fun billingView(billing: List<PayerFieldSpec>): String =
        when (state.billingView(billing)) {
            BillingView.ADD -> "add"
            BillingView.FIELDS -> "fields"
            BillingView.SUMMARY -> "summary"
        }

    /** Shows the next billing address line. */
    public fun addAddressLine(): Unit = state.addAddressLine()

    /** Removes the address line [field]; the lines after it move up. */
    public fun removeAddressLine(field: PayerField): Unit = state.removeAddressLine(field)

    /** Whether [field] is one of the billing address lines. */
    public fun isAddressLine(field: PayerField): Boolean = field.isAddressLine

    /** The payer picked the country [code] for [field]. */
    public fun selectCountry(
        field: PayerField,
        code: String,
    ) {
        state.openCountryPicker(field)
        state.selectCountry(code)
    }

    /** The payer picked the option [code] for [field]. */
    public fun selectOption(
        field: PayerField,
        code: String,
    ) {
        state.optionPickerField = field
        state.selectOption(code)
    }

    public val phoneIso: String? get() = state.phoneCountry?.iso

    public val phoneDialCode: String? get() = state.phoneCountry?.dialCode

    public val phoneText: String get() = PhoneNumbers.format(state.phoneNational, state.phoneCountry)

    public val phonePlaceholder: String get() = PhoneNumbers.placeholder(state.phoneCountry)

    /** The payer typed [text] into the phone field. */
    public fun phoneInput(text: String): Unit = state.onPhoneInput(text)

    /** The payer picked the phone country [iso]. */
    public fun selectPhoneCountry(iso: String): Unit = state.selectPhoneCountry(iso)

    /** The name of [field], without the optional mark. */
    public fun baseLabel(field: PayerField): String = strings.text(labelKey(field))

    /** The label of a field on its own: the name, marked optional when it is. */
    public fun label(spec: PayerFieldSpec): String {
        val base = baseLabel(spec.field)
        return if (spec.mandatory) base else strings.format("payer_optional", base)
    }

    /** The label of a field inside a group: a mandatory one is starred when the group has optional ones too. */
    public fun groupLabel(
        spec: PayerFieldSpec,
        markMandatory: Boolean,
    ): String {
        val name = baseLabel(spec.field)
        return if (spec.mandatory && markMandatory) "$name *" else name
    }

    /** The placeholder of [field]: the format of a date, empty for the rest. */
    public fun hint(field: PayerField): String =
        if (TransferFields.kind(field) == TransferFields.Kind.DATE) strings.text("transfer_date_hint") else ""

    /** `text`, `date`, `accountType`, `idType` or `country`. */
    public fun kind(field: PayerField): String =
        when (TransferFields.kind(field)) {
            TransferFields.Kind.TEXT -> "text"
            TransferFields.Kind.DATE -> "date"
            TransferFields.Kind.ACCOUNT_TYPE -> "accountType"
            TransferFields.Kind.ID_TYPE -> "idType"
            TransferFields.Kind.COUNTRY -> "country"
        }

    /** The name of the field's `TextFieldContentType`, e.g. `EMAIL` or `POSTAL_CODE`. */
    public fun contentType(field: PayerField): String = field.contentType.name

    /** The codes a picker [field] offers. */
    public fun options(field: PayerField): List<String> = TransferFields.options(field)

    /** The words for an option [code]; the code itself when it has none. */
    public fun optionLabel(code: String): String {
        val key =
            when {
                code in TransferFields.ACCOUNT_TYPES -> "transfer_account_type_$code"
                code in TransferFields.ID_TYPES -> "transfer_id_type_${code.removePrefix("IDTP")}"
                else -> return code
            }
        return strings.text(key)
    }

    /** The name of the country [code] in the checkout's language. */
    public fun countryName(code: String): String = countryDisplayName(code, strings.language)

    /** The flag emoji of the country [iso]. */
    public fun flag(iso: String): String = flagEmoji(iso)

    /** Every country of the country picker, by name. */
    public fun countries(): List<CountryOptionSpec> =
        Countries.codes
            .map { CountryOptionSpec(it, countryDisplayName(it, strings.language), countryDisplayName(it, "en"), null) }
            .sortedBy { it.name.lowercase() }

    /** Every country of the phone country picker, by name, with its dial code. */
    public fun phoneCountries(): List<CountryOptionSpec> =
        PhoneCountries.all
            .map { CountryOptionSpec(it.iso, countryDisplayName(it.iso, strings.language), countryDisplayName(it.iso, "en"), it.dialCode) }
            .sortedBy { it.name.lowercase() }

    public val suggestedPhoneCountries: List<String> get() = state.suggestedPhoneCountries.map { it.iso }

    /** The billing address of [specs] the payer filled in, on one line. */
    public fun billingSummary(specs: List<PayerFieldSpec>): String {
        val asked = specs.map { it.field }.toSet()

        fun known(field: PayerField) = state.value(field).trim().takeIf { field in asked && it.isNotEmpty() }
        return listOfNotNull(
            known(PayerField.BILLING_ADDRESS_LINE1),
            known(PayerField.BILLING_ADDRESS_LINE2),
            known(PayerField.BILLING_ADDRESS_LINE3),
            listOfNotNull(known(PayerField.BILLING_CITY), known(PayerField.BILLING_POSTAL_CODE)).joinToString(" ").ifEmpty { null },
            known(PayerField.BILLING_STATE),
            known(PayerField.BILLING_COUNTRY)?.let { countryDisplayName(it, strings.language) },
        ).joinToString(", ")
    }

    /** The folded payer data of [specs]: e-mail, phone and address, each when filled in. */
    public fun summaryLines(specs: List<PayerFieldSpec>): List<PayerSummaryLine> {
        val asked = specs.map { it.field }.toSet()

        fun known(field: PayerField) = state.value(field).trim().takeIf { field in asked && it.isNotEmpty() }
        val phone =
            known(PayerField.MOBILE_PHONE)?.let {
                val country = state.phoneCountry ?: return@let ltr(it)
                ltr("+${country.dialCode} ${PhoneNumbers.format(state.phoneNational, country)}")
            }
        val address = billingSummary(specs)
        return listOfNotNull(
            known(PayerField.EMAIL)?.let { PayerSummaryLine("mail", it) },
            phone?.let { PayerSummaryLine("phone", it) },
            address.takeIf { it.isNotEmpty() }?.let { PayerSummaryLine("pin", it) },
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private fun labelKey(field: PayerField): String =
        when (field) {
            PayerField.EMAIL -> "card_form_email_label"
            PayerField.MOBILE_PHONE -> "card_form_phone_label"
            PayerField.BILLING_COUNTRY, PayerField.RECIPIENT_COUNTRY -> "payer_country_label"
            PayerField.BILLING_STATE, PayerField.RECIPIENT_STATE -> "payer_state_label"
            PayerField.BILLING_CITY, PayerField.RECIPIENT_CITY -> "payer_city_label"
            PayerField.BILLING_POSTAL_CODE, PayerField.RECIPIENT_POSTAL_CODE -> "payer_postal_code_label"
            PayerField.BILLING_ADDRESS_LINE1 -> "payer_address_line1_label"
            PayerField.BILLING_ADDRESS_LINE2 -> "payer_address_line2_label"
            PayerField.BILLING_ADDRESS_LINE3 -> "payer_address_line3_label"
            PayerField.PAYER_FIRST_NAME, PayerField.RECIPIENT_FIRST_NAME -> "transfer_first_name"
            PayerField.PAYER_MIDDLE_NAME, PayerField.RECIPIENT_MIDDLE_NAME -> "transfer_middle_name"
            PayerField.PAYER_LAST_NAME, PayerField.RECIPIENT_LAST_NAME -> "transfer_last_name"
            PayerField.PAYER_BIRTHDAY, PayerField.RECIPIENT_BIRTHDAY -> "transfer_birthday"
            PayerField.PAYER_ACCOUNT, PayerField.RECIPIENT_ACCOUNT -> "transfer_account"
            PayerField.PAYER_ACCOUNT_NUMBER_TYPE, PayerField.RECIPIENT_ACCOUNT_NUMBER_TYPE -> "transfer_account_type"
            PayerField.PAYER_ID_TYPE, PayerField.RECIPIENT_ID_TYPE -> "transfer_id_type"
            PayerField.PAYER_ID_NUMBER, PayerField.RECIPIENT_ID_NUMBER -> "transfer_id_number"
            PayerField.RECIPIENT_ADDRESS_LINE1 -> "transfer_address"
        }
}

/** Makes the [PayerForm] of a card step. */
@PayrdrUiApi
public object PayerForms {
    /** The payer fields of a card step; with a saved [card] they start from what was sent with it before. */
    public fun of(
        request: PayerDataRequest,
        card: SavedCardInfo?,
        strings: PaymentStrings,
    ): PayerForm = PayerForm(request, card?.payerPrefill.orEmpty(), strings)

    /** [text] as one left-to-right run, for masks, amounts and codes inside right-to-left text. */
    public fun leftToRight(text: String): String = ltr(text)
}
