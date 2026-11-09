package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.payrdr.mobile.payment.sdk.payment.result.PayerDataRequest
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneCountries
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneCountry
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneNumbers
import net.payrdr.mobile.payment.sdk.payment.ui.util.FieldError
import net.payrdr.mobile.payment.sdk.payment.ui.util.PayerFieldValidation
import net.payrdr.mobile.payment.sdk.payment.ui.util.deviceRegion

/**
 * Values, visibility and errors of the payer fields the gateway asks for. Which fields are shown
 * depends on the card's payment system, so every query takes the gateway names of that system.
 *
 * The phone is kept as a country plus national digits; [value] of [PayerField.MOBILE_PHONE] is the E.164
 * number built from them.
 *
 * @param prefillOverride values remembered with a saved card; win over [PayerDataRequest.prefill]
 * @param deviceRegion region of the device (ISO alpha-2), the phone country when the gateway suggests none
 */
@Stable
internal class PayerFieldsState(
    private val request: PayerDataRequest,
    prefillOverride: Map<PayerField, String> = emptyMap(),
    deviceRegion: String? = null,
) {
    private val prefill: Map<PayerField, String> = request.prefill + prefillOverride.filterValues { it.isNotBlank() }

    // Dates are kept as typed (DD.MM.YYYY) and turned back into the gateway's YYYYMMDD in submitValues.
    private val values =
        mutableStateMapOf<PayerField, String>().apply {
            putAll(prefill.mapValues { (field, value) -> if (field.isDate) TransferFields.dateForInput(value) else value })
        }

    /** Countries offered first in the phone code picker: the gateway's suggestion, then the device region. */
    val suggestedPhoneCountries: List<PhoneCountry> =
        listOfNotNull(request.phoneRegion, deviceRegion).mapNotNull(PhoneCountries::byIso).distinctBy { it.iso }

    var phoneCountry: PhoneCountry? by mutableStateOf(suggestedPhoneCountries.firstOrNull())
        private set

    /** National digits of the phone, without the calling code. */
    var phoneNational: String by mutableStateOf("")
        private set

    var phonePickerOpen: Boolean by mutableStateOf(false)

    init {
        prefill[PayerField.MOBILE_PHONE]?.let { onPhoneInput(it) }
    }

    private val visited = mutableSetOf<PayerField>()

    /** Fields whose error may be shown: the user left them, picked a value or tried to pay. */
    var touched: Set<PayerField> by mutableStateOf(emptySet())
        private set

    /** How many optional address lines after the first the user revealed with "Add address line". */
    private var extraAddressLines by mutableIntStateOf(0)

    var countryPickerOpen: Boolean by mutableStateOf(false)

    /** The field the country picker fills: the billing country, or the recipient's country of a transfer. */
    var countryPickerField: PayerField by mutableStateOf(PayerField.BILLING_COUNTRY)
        private set

    /** The choice field (account or document type of a transfer) whose list is open, or null. */
    var optionPickerField: PayerField? by mutableStateOf(null)

    fun openCountryPicker(field: PayerField) {
        countryPickerField = field
        countryPickerOpen = true
    }

    /** Picks [code] for the open choice field. */
    fun selectOption(code: String) {
        val field = optionPickerField ?: return
        values[field] = code
        touched = touched + field
        optionPickerField = null
    }

    /** Fields to show for a card of the first of [paymentSystems] the gateway configured, in display order. */
    fun specsFor(paymentSystems: List<String>): List<PayerFieldSpec> {
        val requested = requested(paymentSystems)
        val lines = requested.filter { it.field in ADDRESS_LINES }
        return requested.filter { it.field !in ADDRESS_LINES || isLineShown(it, lines.indexOf(it)) }
    }

    /** True while some requested address line is still hidden behind "Add address line". */
    fun canAddAddressLine(paymentSystems: List<String>): Boolean {
        val lines = requested(paymentSystems).filter { it.field in ADDRESS_LINES }
        return lines.withIndex().any { (index, spec) -> !isLineShown(spec, index) }
    }

    fun addAddressLine() {
        extraAddressLines++
    }

    /** Drops a revealed address line: the lines after it move up and the last one hides again. */
    fun removeAddressLine(field: PayerField) {
        val lines = ADDRESS_LINES.toList()
        val from = lines.indexOf(field)
        if (from <= 0) return
        for (i in from until lines.lastIndex) values[lines[i]] = value(lines[i + 1])
        values.remove(lines.last())
        touched = touched - lines.drop(from).toSet()
        if (extraAddressLines > 0) extraAddressLines--
    }

    fun value(field: PayerField): String = values[field].orEmpty()

    fun onValueChange(
        field: PayerField,
        value: String,
    ) {
        if (field == PayerField.MOBILE_PHONE) {
            onPhoneInput(value)
        } else if (field.isDate) {
            values[field] = TransferFields.formatDateInput(value, TransferFields.today())
        } else {
            values[field] = value.take(PayerFieldValidation.maxLength(field))
        }
    }

    /** Typed, pasted or autofilled phone text; an international number also switches the country. */
    fun onPhoneInput(text: String) {
        val (country, national) = PhoneNumbers.input(text, phoneCountry)
        phoneCountry = country
        setPhone(national)
    }

    fun selectPhoneCountry(iso: String) {
        phoneCountry = PhoneCountries.byIso(iso) ?: return
        // Keep what was typed: the digits stay, re-normalised for the new country.
        setPhone(PhoneNumbers.national(phoneNational, phoneCountry))
        phonePickerOpen = false
    }

    private fun setPhone(national: String) {
        phoneNational = national
        values[PayerField.MOBILE_PHONE] = PhoneNumbers.e164(phoneCountry, national)
    }

    /** Read-only only when the gateway forbids editing *and* there is a value to show. */
    fun isEditable(spec: PayerFieldSpec): Boolean = spec.editable || value(spec.field).isBlank()

    fun onFocusChange(
        field: PayerField,
        focused: Boolean,
    ) {
        if (focused) {
            visited += field
            return
        }
        // Focus callbacks also fire with false on first composition — only a field the user was in counts as left.
        if (field !in visited) return
        touched = touched + field
    }

    fun selectCountry(code: String) {
        values[countryPickerField] = code
        touched = touched + countryPickerField
        countryPickerOpen = false
    }

    /** Error of [spec], or null when valid. */
    fun errorOf(spec: PayerFieldSpec): FieldError? = PayerFieldValidation.validate(spec, value(spec.field))

    /** A check to display: the user left the field filled in and correct. */
    fun isVisiblyValid(spec: PayerFieldSpec): Boolean = spec.field in touched && value(spec.field).isNotBlank() && errorOf(spec) == null

    /** Error to display — only for [touched] fields. */
    fun visibleErrorOf(spec: PayerFieldSpec): FieldError? = if (spec.field in touched) errorOf(spec) else null

    /** Marks every shown field touched; true when all of them are valid. */
    fun validateAll(specs: List<PayerFieldSpec>): Boolean {
        touched = touched + specs.map { it.field }
        return specs.all { errorOf(it) == null }
    }

    /**
     * Where an all-optional billing block rests: open while a value would not pass, the address in one line
     * when there is one, otherwise a row to add it.
     */
    fun billingView(billing: List<PayerFieldSpec>): BillingView =
        when {
            billing.any { errorOf(it) != null } -> BillingView.FIELDS
            billing.any { value(it.field).isNotBlank() } -> BillingView.SUMMARY
            else -> BillingView.ADD
        }

    /** Non-blank values of the shown fields, to send with the payment. */
    fun submitValues(specs: List<PayerFieldSpec>): Map<PayerField, String> =
        specs
            .mapNotNull { spec ->
                value(spec.field)
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { spec.field to if (spec.field.isDate) TransferFields.dateForApi(it) else it }
            }.toMap()

    private fun requested(paymentSystems: List<String>): List<PayerFieldSpec> =
        paymentSystems
            .firstNotNullOfOrNull { system -> request.fieldsFor(system).takeIf { it.isNotEmpty() } }
            .orEmpty()
            .sortedBy { DISPLAY_ORDER.indexOf(it.field) }

    /** The first requested address line is always shown; the next ones once added, prefilled or mandatory. */
    private fun isLineShown(
        spec: PayerFieldSpec,
        index: Int,
    ): Boolean = index <= extraAddressLines || spec.mandatory || value(spec.field).isNotBlank()

    private companion object {
        // In order: removing a line shifts the next ones up.
        val ADDRESS_LINES =
            linkedSetOf(PayerField.BILLING_ADDRESS_LINE1, PayerField.BILLING_ADDRESS_LINE2, PayerField.BILLING_ADDRESS_LINE3)

        // Contacts, the billing address, the sender of a transfer, the recipient.
        val DISPLAY_ORDER =
            listOf(PayerField.EMAIL, PayerField.MOBILE_PHONE) +
                listOf(
                    PayerField.BILLING_COUNTRY,
                    PayerField.BILLING_ADDRESS_LINE1,
                    PayerField.BILLING_ADDRESS_LINE2,
                    PayerField.BILLING_ADDRESS_LINE3,
                    PayerField.BILLING_CITY,
                    PayerField.BILLING_POSTAL_CODE,
                    PayerField.BILLING_STATE,
                ) +
                TransferFields.SENDER +
                TransferFields.RECIPIENT

        val PayerField.isDate: Boolean get() = TransferFields.kind(this) == TransferFields.Kind.DATE
    }
}

/** What an all-optional billing block of the new-card form shows. */
internal enum class BillingView {
    /** A single "Add billing details" row. */
    ADD,

    /** The fields themselves, with "Hide". */
    FIELDS,

    /** The entered address in one line, with "Edit". */
    SUMMARY,
}

@Composable
internal fun rememberPayerFieldsState(
    request: PayerDataRequest,
    prefillOverride: Map<PayerField, String> = emptyMap(),
): PayerFieldsState = remember(request, prefillOverride) { PayerFieldsState(request, prefillOverride, deviceRegion()) }
