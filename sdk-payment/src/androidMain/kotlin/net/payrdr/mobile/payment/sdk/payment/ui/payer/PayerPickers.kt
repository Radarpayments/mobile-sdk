package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.payrdr.mobile.payment.sdk.payment.flow.Countries
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.ui.LocalSdkLanguage
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldBox
import net.payrdr.mobile.payment.sdk.payment.ui.components.NativeTextField
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.SheetOverlay
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextFieldContentType
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneCountries
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular
import net.payrdr.mobile.payment.sdk.payment.ui.util.FlagIcon
import net.payrdr.mobile.payment.sdk.payment.ui.util.countryDisplayName
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_country_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_country_not_found
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_country_search
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.phone_code_search
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.phone_code_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.picker_all
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.picker_suggested
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource

/**
 * Pickers of the payer fields (billing or recipient country, phone code, account and document types of a transfer);
 * draw it in the screen overlay.
 */
@Composable
internal fun CountryPickerSheet(state: PayerFieldsState) {
    val language = LocalSdkLanguage.current
    // ~500 platform name lookups: built in the background so the form opens without them.
    val billing by
        produceState(emptyList<CountryOption>(), language) {
            value =
                withContext(Dispatchers.Default) {
                    Countries.codes
                        .map {
                            CountryOption(
                                it,
                                countryDisplayName(it, language),
                                countryDisplayName(it, "en"),
                            )
                        }.sortedBy { it.name.lowercase() }
                }
        }
    val phone by
        produceState(emptyList<CountryOption>(), language) {
            value =
                withContext(Dispatchers.Default) {
                    PhoneCountries.all
                        .map { CountryOption(it.iso, countryDisplayName(it.iso, language), countryDisplayName(it.iso, "en"), it.dialCode) }
                        .sortedBy { it.name.lowercase() }
                }
        }
    val suggestedPhone = remember(phone) { state.suggestedPhoneCountries.mapNotNull { c -> phone.firstOrNull { it.code == c.iso } } }
    CountryListSheet(
        visible = state.countryPickerOpen,
        title = stringResource(Res.string.payer_country_label),
        searchHint = stringResource(Res.string.payer_country_search),
        countries = billing,
        suggested = emptyList(),
        selected = state.value(state.countryPickerField),
        onSelect = state::selectCountry,
        onDismiss = { state.countryPickerOpen = false },
    )
    CountryListSheet(
        visible = state.phonePickerOpen,
        title = stringResource(Res.string.phone_code_title),
        searchHint = stringResource(Res.string.phone_code_search),
        countries = phone,
        suggested = suggestedPhone,
        selected = state.phoneCountry?.iso,
        onSelect = state::selectPhoneCountry,
        onDismiss = { state.phonePickerOpen = false },
    )
    OptionListSheet(state)
}

/** The codes of the open choice field of a transfer, by their labels. */
@Composable
private fun OptionListSheet(state: PayerFieldsState) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    // Kept while the sheet slides away, so its content does not vanish mid-animation.
    var shownField by remember { mutableStateOf<PayerField?>(null) }
    state.optionPickerField?.let { shownField = it }
    val field = shownField
    SheetOverlay(visible = state.optionPickerField != null, onDismiss = { state.optionPickerField = null }) {
        if (field == null) return@SheetOverlay
        Text(baseLabelOf(field), style = typography.headline.copy(color = colors.onBackground))
        val selected = state.value(field)
        LazyColumn(modifier = Modifier.fillMaxWidth().selectableGroup()) {
            items(TransferFields.options(field), key = { it }) { code ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .selectable(selected = code == selected, role = Role.RadioButton) { state.selectOption(code) }
                            .padding(horizontal = 4.dp, vertical = 12.dp)
                            .testTag("transfer_option_$code"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        optionLabelOf(code),
                        style = typography.body.copy(color = colors.onBackground),
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        if (code ==
                            selected
                        ) {
                            Icon(PaymentIcons.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Searchable list of countries with flags; [suggested] ones are listed first while the search is empty. */
@Composable
private fun CountryListSheet(
    visible: Boolean,
    title: String,
    searchHint: String,
    countries: List<CountryOption>,
    suggested: List<CountryOption>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    var query by remember { mutableStateOf("") }
    SheetOverlay(
        visible = visible,
        onDismiss = {
            onDismiss()
            query = ""
        },
    ) {
        Text(title, style = typography.headline.copy(color = colors.onBackground))
        var searchFocused by remember { mutableStateOf(false) }
        FieldBox(focused = searchFocused, isError = false) {
            Icon(PaymentIcons.Search, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
            NativeTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = searchHint,
                keyboardOptions = KeyboardOptions.Default,
                contentType = TextFieldContentType.NONE,
                enabled = true,
                onFocusChange = { searchFocused = it },
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 6.dp),
                label = searchHint,
            )
        }
        val needle = query.trim()
        val shown = countries.filter { it.matches(needle) }
        val pick = { code: String ->
            onSelect(code)
            query = ""
        }
        // The list takes all the room the sheet has: the sheet then always spans the screen, its top stays
        // put while filtering and when the keyboard opens — the keyboard only shortens the list.
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).selectableGroup()) {
            if (shown.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.payer_country_not_found),
                        style = typography.body.copy(color = colors.onSurfaceVariant),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                    )
                }
            }
            if (needle.isEmpty() && suggested.isNotEmpty()) {
                item { ListHeader(stringResource(Res.string.picker_suggested)) }
                items(suggested, key = { "suggested-${it.code}" }) { CountryRow(it, it.code == selected) { pick(it.code) } }
                item { ListHeader(stringResource(Res.string.picker_all)) }
            }
            items(shown, key = { it.code }) { CountryRow(it, it.code == selected) { pick(it.code) } }
        }
    }
}

@Composable
private fun ListHeader(text: String) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Text(
        text.uppercase(),
        style = typography.caption.copy(color = colors.onSurfaceVariant, letterSpacing = 0.6.sp),
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun CountryRow(
    country: CountryOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                // Read as "selected" or not, like the check mark shows it.
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlagIcon(country.code)
        Text(
            country.name,
            style = typography.body.copy(color = colors.onBackground),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        country.dialCode?.let { Text(ltr("+$it"), style = typography.body.tabular().copy(color = colors.onSurfaceVariant)) }
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            if (selected) Icon(PaymentIcons.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
        }
    }
}

/** A country in a picker: found by its name in the UI language, its English name, its code or calling code. */
private data class CountryOption(
    val code: String,
    val name: String,
    val englishName: String,
    val dialCode: String? = null,
) {
    fun matches(query: String): Boolean {
        if (query.isEmpty()) return true
        val digits = query.removePrefix("+")
        return name.contains(query, ignoreCase = true) ||
            englishName.contains(query, ignoreCase = true) ||
            code.equals(query, ignoreCase = true) ||
            (dialCode != null && digits.isNotEmpty() && digits.all(Char::isDigit) && dialCode.startsWith(digits))
    }
}
