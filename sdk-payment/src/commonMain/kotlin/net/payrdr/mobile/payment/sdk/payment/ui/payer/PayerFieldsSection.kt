package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.payrdr.mobile.payment.sdk.payment.flow.Countries
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.LocalSdkLanguage
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldBox
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldGroupFrame
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldGroupHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.FormField
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupButtonCell
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupCellDivider
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupCellEdges
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupFieldStatus
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupPickerContent
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupRow
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupRowDivider
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupTextCell
import net.payrdr.mobile.payment.sdk.payment.ui.components.NativeTextField
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.SheetOverlay
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextAction
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextFieldContentType
import net.payrdr.mobile.payment.sdk.payment.ui.components.keepVisibleWhileFocused
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneCountries
import net.payrdr.mobile.payment.sdk.payment.ui.payer.phone.PhoneNumbers
import net.payrdr.mobile.payment.sdk.payment.ui.screens.message
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular
import net.payrdr.mobile.payment.sdk.payment.ui.util.FlagIcon
import net.payrdr.mobile.payment.sdk.payment.ui.util.countryDisplayName
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_email_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_phone_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_add_address_line
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_address_line1_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_address_line2_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_address_line3_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_billing_add
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_billing_help
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_billing_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_city_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_country_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_country_not_found
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_country_search
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_optional
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_optional_note
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_postal_code_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_remove_address_line
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_state_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_summary_collapse
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.phone_code_cd
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.phone_code_choose
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.phone_code_search
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.phone_code_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.picker_all
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.picker_suggested
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_1
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_100
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_2
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_3
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_4
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_5
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_6
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_7
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_8
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_address
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_birthday
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_date_hint
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_first_name
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_number
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_1
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_10
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_11
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_2
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_3
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_4
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_5
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_6
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_7
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_8
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_id_type_9
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_last_name
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_middle_name
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_recipient_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_sender_title
import org.jetbrains.compose.resources.stringResource

/**
 * Payer fields the gateway asks for a card of [paymentSystems] (gateway names, see
 * [PayerFieldsState.specsFor]): contacts continue the card fields, the billing address is a block behind
 * a hairline; a transfer adds a block for its sender and then one for its recipient after the address.
 * Renders nothing when nothing is asked. Pair with [CountryPickerSheet] in the screen's overlay.
 *
 * @param collapsibleBilling an all-optional billing block starts as a row to add it, see [CollapsibleBillingGroup]
 */
@Composable
internal fun PayerFieldsSection(
    state: PayerFieldsState,
    paymentSystems: List<String>,
    modifier: Modifier = Modifier,
    collapsibleBilling: Boolean = false,
) {
    val specs = state.specsFor(paymentSystems)
    Column(modifier = modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val contact = specs.filter { it.field == PayerField.EMAIL || it.field == PayerField.MOBILE_PHONE }
        val sender = specs.filter { it.field in TransferFields.SENDER }
        val recipient = specs.filter { it.field in TransferFields.RECIPIENT }
        val billing = specs - (contact + sender + recipient).toSet()
        contact.forEach { if (it.field == PayerField.MOBILE_PHONE) PhoneField(state, it) else PayerTextField(state, it) }
        if (billing.isNotEmpty()) {
            val canAddLine = state.canAddAddressLine(paymentSystems)
            if (collapsibleBilling && billing.none { it.mandatory }) {
                CollapsibleBillingGroup(state = state, billing = billing, canAddLine = canAddLine)
            } else {
                BillingGroup(state = state, billing = billing, canAddLine = canAddLine)
            }
        }
        if (sender.isNotEmpty()) TransferGroup(state, stringResource(Res.string.transfer_sender_title), sender, "transfer_sender")
        if (recipient.isNotEmpty()) {
            TransferGroup(
                state,
                stringResource(Res.string.transfer_recipient_title),
                recipient,
                "transfer_recipient",
            )
        }
    }
}

/**
 * The sender or the recipient of a transfer as one framed group, a field per row: text, a date typed `DD/MM/YYYY`, a
 * country, or a code picked from a list (account and document types).
 */
@Composable
private fun TransferGroup(
    state: PayerFieldsState,
    title: String,
    specs: List<PayerFieldSpec>,
    tag: String,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    var focused by remember { mutableStateOf<PayerField?>(null) }
    val someOptional = specs.any { !it.mandatory }
    val error = specs.firstNotNullOfOrNull { state.visibleErrorOf(it) }

    fun highlightOf(spec: PayerFieldSpec): Color? =
        when {
            state.visibleErrorOf(spec) != null -> colors.error
            focused == spec.field -> colors.primary
            else -> null
        }

    Column(
        modifier = Modifier.fillMaxWidth().keepVisibleWhileFocused(focused != null).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FieldGroupHeader(
            title = title,
            trailing =
                if (specs.none { it.mandatory }) {
                    {
                        Text(
                            stringResource(Res.string.payer_optional_note),
                            style = typography.caption.copy(color = colors.onSurfaceVariant),
                        )
                    }
                } else {
                    null
                },
        )
        FieldGroupFrame(labeled = true) {
            specs.forEachIndexed { row, spec ->
                if (row > 0) GroupRowDivider()
                GroupRow(highlighted = highlightOf(spec) != null) {
                    val edges = GroupCellEdges.of(row = row, rows = specs.size)
                    val cellModifier = Modifier.weight(1f)
                    when (TransferFields.kind(spec.field)) {
                        TransferFields.Kind.COUNTRY -> CountryCell(state, spec, highlightOf(spec), edges, cellModifier, someOptional)
                        TransferFields.Kind.ACCOUNT_TYPE, TransferFields.Kind.ID_TYPE ->
                            OptionCell(state, spec, highlightOf(spec), edges, cellModifier, someOptional)
                        TransferFields.Kind.TEXT, TransferFields.Kind.DATE -> {
                            val text = groupTextOf(spec, someOptional)
                            GroupTextCell(
                                value = state.value(spec.field),
                                onValueChange = { state.onValueChange(spec.field, it) },
                                placeholder = text.label,
                                hint = text.hint,
                                keyboardOptions =
                                    if (TransferFields.kind(spec.field) == TransferFields.Kind.DATE) {
                                        KeyboardOptions(keyboardType = KeyboardType.Number)
                                    } else {
                                        KeyboardOptions.Default
                                    },
                                contentType = spec.field.contentType,
                                status = groupStatusOf(state, spec),
                                highlight = highlightOf(spec),
                                edges = edges,
                                onFocusChange = { isFocused ->
                                    if (isFocused) {
                                        focused = spec.field
                                    } else if (focused == spec.field) {
                                        focused = null
                                    }
                                    state.onFocusChange(spec.field, isFocused)
                                },
                                enabled = state.isEditable(spec),
                                modifier = cellModifier.testTag("payer_${spec.field.apiName}"),
                            )
                        }
                    }
                }
            }
        }
        if (error != null) {
            Text(text = stringResource(error.message()), style = typography.caption.copy(color = colors.error))
        }
    }
}

/** An account or document type: the label of the picked code (or the code the merchant passed), opening the list. */
@Composable
private fun OptionCell(
    state: PayerFieldsState,
    spec: PayerFieldSpec,
    highlight: Color?,
    edges: GroupCellEdges,
    modifier: Modifier,
    markMandatory: Boolean,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    val code = state.value(spec.field)
    val editable = state.isEditable(spec)
    GroupButtonCell(
        onClick = { state.optionPickerField = spec.field },
        highlight = highlight,
        edges = edges,
        enabled = editable,
        modifier = modifier.testTag("payer_${spec.field.apiName}"),
    ) {
        GroupPickerContent(
            label = groupTextOf(spec, markMandatory).label,
            isError = state.visibleErrorOf(spec) != null,
            value =
                if (code.isEmpty()) {
                    null
                } else {
                    {
                        Text(
                            text = optionLabelOf(code),
                            style = typography.input.copy(color = colors.onBackground),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
        )
        if (editable) {
            // In the 28dp slot of "✕" and "+" of the address lines, so that all of them stand on one line.
            Icon(
                PaymentIcons.ChevronDown,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(28.dp).padding(4.dp),
            )
        }
    }
}

/**
 * Optional billing fields kept out of the way: a row to add them, the group with "Hide", or the entered
 * address in one line with "Edit". A wrong value opens the group (Pay marks every field), and "Hide" closes it
 * only once the values pass — hiding one would send it unseen.
 */
@Composable
private fun CollapsibleBillingGroup(
    state: PayerFieldsState,
    billing: List<PayerFieldSpec>,
    canAddLine: Boolean,
) {
    var view by remember(state) { mutableStateOf(state.billingView(billing)) }
    // Set by the add row: the first typed field takes the cursor once the group appears.
    var focusOnOpen by remember { mutableStateOf<PayerField?>(null) }
    val hasError = billing.any { state.visibleErrorOf(it) != null }
    LaunchedEffect(hasError) { if (hasError) view = BillingView.FIELDS }
    AnimatedContent(
        targetState = view,
        transitionSpec = { summaryTransition() },
        label = "billing",
        modifier = Modifier.fillMaxWidth(),
    ) { shown ->
        when (shown) {
            BillingView.ADD ->
                AddBillingRow(onClick = {
                    focusOnOpen = billing.firstOrNull { it.field != PayerField.BILLING_COUNTRY }?.field
                    view = BillingView.FIELDS
                })
            BillingView.FIELDS ->
                BillingGroup(
                    state = state,
                    billing = billing,
                    canAddLine = canAddLine,
                    initialFocus = focusOnOpen,
                    onHide = {
                        focusOnOpen = null
                        if (state.validateAll(billing)) view = state.billingView(billing)
                    },
                )
            BillingView.SUMMARY ->
                PayerSummaryCard(
                    title = stringResource(Res.string.payer_billing_title),
                    lines = listOf(SummaryLine(PaymentIcons.Pin, billingSummary(state, billing))),
                    onEdit = { view = BillingView.FIELDS },
                    modifier = Modifier.testTag("payer_billing_summary"),
                )
        }
    }
}

/** "+ Add billing details · optional" as a one-row group, in place of the billing fields. */
@Composable
private fun AddBillingRow(onClick: () -> Unit) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    FieldGroupFrame {
        GroupRow(highlighted = false) {
            GroupButtonCell(
                onClick = onClick,
                highlight = null,
                edges = GroupCellEdges.of(row = 0, rows = 1),
                modifier = Modifier.weight(1f).testTag("payer_billing_add"),
            ) {
                Icon(PaymentIcons.Plus, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(Res.string.payer_billing_add),
                    style = typography.input.copy(color = colors.primary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Text(
                    stringResource(Res.string.payer_optional_note),
                    style = typography.caption.copy(color = colors.onSurfaceVariant),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/**
 * Billing fields as one framed group: country, the address lines, a row that reveals the next address line,
 * city | postal code, state. Fields the gateway did not ask for are left out; a lone field takes the whole row.
 *
 * @param initialFocus field that takes the cursor when the group appears
 * @param onHide when non-null, the header offers "Hide" in place of the "optional" note
 */
@Composable
private fun BillingGroup(
    state: PayerFieldsState,
    billing: List<PayerFieldSpec>,
    canAddLine: Boolean,
    initialFocus: PayerField? = null,
    onHide: (() -> Unit)? = null,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    var focused by remember { mutableStateOf<PayerField?>(null) }
    // The address line revealed by the last tap on the add row: it takes the cursor.
    var lineToFocus by remember { mutableStateOf(initialFocus) }
    val someOptional = billing.any { !it.mandatory }

    fun specOf(field: PayerField) = billing.firstOrNull { it.field == field }

    val rows: List<List<BillingCell>> =
        buildList {
            specOf(PayerField.BILLING_COUNTRY)?.let { add(listOf(BillingCell.Input(it))) }
            billing.filter { it.field.isAddressLine }.forEach { add(listOf(BillingCell.Input(it))) }
            if (canAddLine) add(listOf(BillingCell.AddLine))
            listOfNotNull(specOf(PayerField.BILLING_CITY), specOf(PayerField.BILLING_POSTAL_CODE))
                .takeIf { it.isNotEmpty() }
                ?.let { pair -> add(pair.map { BillingCell.Input(it) }) }
            specOf(PayerField.BILLING_STATE)?.let { add(listOf(BillingCell.Input(it))) }
        }
    val error = billing.firstNotNullOfOrNull { state.visibleErrorOf(it) }

    fun highlightOf(cell: BillingCell): Color? {
        val spec = (cell as? BillingCell.Input)?.spec ?: return null
        return when {
            state.visibleErrorOf(spec) != null -> colors.error
            focused == spec.field -> colors.primary
            else -> null
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().keepVisibleWhileFocused(focused != null),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FieldGroupHeader(
            title = stringResource(Res.string.payer_billing_title),
            trailing =
                if (onHide != null) {
                    {
                        TextAction(
                            text = stringResource(Res.string.payer_summary_collapse),
                            onClick = onHide,
                            // Line the text up with the group's right edge: TextAction carries its own 8dp padding.
                            modifier = Modifier.offset(x = 8.dp),
                        )
                    }
                } else if (billing.none { it.mandatory }) {
                    {
                        Text(
                            stringResource(Res.string.payer_optional_note),
                            style = typography.caption.copy(color = colors.onSurfaceVariant),
                        )
                    }
                } else {
                    null
                },
        )
        // Before the fields, not under them: read before typing rather than after an error.
        Text(
            text = stringResource(Res.string.payer_billing_help),
            style = typography.caption.copy(color = colors.onSurfaceVariant),
        )
        FieldGroupFrame(labeled = true) {
            rows.forEachIndexed { row, cells ->
                if (row > 0) GroupRowDivider()
                GroupRow(highlighted = cells.any { highlightOf(it) != null }) {
                    cells.forEachIndexed { column, cell ->
                        if (column > 0) GroupCellDivider()
                        val edges = GroupCellEdges.of(row = row, rows = rows.size, column = column, columns = cells.size)
                        val cellModifier = Modifier.weight(1f)
                        when (cell) {
                            BillingCell.AddLine ->
                                GroupButtonCell(
                                    onClick = {
                                        val shown = billing.map { it.field }
                                        lineToFocus = EXTRA_ADDRESS_LINES.firstOrNull { it !in shown }
                                        state.addAddressLine()
                                    },
                                    highlight = null,
                                    edges = edges,
                                    modifier = cellModifier,
                                ) {
                                    // Text starts where the hints of the other cells do; "+" sits where "✕" of an added line is.
                                    Text(
                                        stringResource(Res.string.payer_add_address_line),
                                        style = typography.input.copy(color = colors.primary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        PaymentIcons.Plus,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(28.dp).padding(5.dp),
                                    )
                                }
                            is BillingCell.Input ->
                                if (cell.spec.field == PayerField.BILLING_COUNTRY) {
                                    CountryCell(state, cell.spec, highlightOf(cell), edges, cellModifier, someOptional)
                                } else {
                                    BillingTextCell(
                                        state = state,
                                        spec = cell.spec,
                                        highlight = highlightOf(cell),
                                        edges = edges,
                                        modifier = cellModifier,
                                        markMandatory = someOptional,
                                        autoFocus = cell.spec.field == lineToFocus,
                                        onFocusChange = { field, isFocused ->
                                            if (isFocused) {
                                                focused = field
                                                if (field == lineToFocus) lineToFocus = null
                                            } else if (focused == field) {
                                                focused = null
                                            }
                                        },
                                    )
                                }
                        }
                    }
                }
            }
        }
        if (error != null) {
            Text(text = stringResource(error.message()), style = typography.caption.copy(color = colors.error))
        }
    }
}

private val EXTRA_ADDRESS_LINES = listOf(PayerField.BILLING_ADDRESS_LINE2, PayerField.BILLING_ADDRESS_LINE3)

private sealed interface BillingCell {
    data class Input(
        val spec: PayerFieldSpec,
    ) : BillingCell

    data object AddLine : BillingCell
}

@Composable
private fun BillingTextCell(
    state: PayerFieldsState,
    spec: PayerFieldSpec,
    highlight: Color?,
    edges: GroupCellEdges,
    modifier: Modifier,
    markMandatory: Boolean,
    autoFocus: Boolean,
    onFocusChange: (PayerField, Boolean) -> Unit,
) {
    val colors = LocalPaymentColors.current
    val field = spec.field
    val editable = state.isEditable(spec)
    // Revealed lines can be taken back; the first line, mandatory and merchant-given ones stay.
    val removable = field != PayerField.BILLING_ADDRESS_LINE1 && field.isAddressLine && !spec.mandatory && editable
    GroupTextCell(
        value = state.value(field),
        onValueChange = { state.onValueChange(field, it) },
        placeholder = groupTextOf(spec, markMandatory).label,
        keyboardOptions = KeyboardOptions.Default,
        contentType = field.contentType,
        // No check mark: nobody verifies an address, a tick would suggest otherwise.
        status = groupStatusOf(state, spec),
        highlight = highlight,
        edges = edges,
        onFocusChange = {
            onFocusChange(field, it)
            state.onFocusChange(field, it)
        },
        enabled = editable,
        autoFocus = autoFocus,
        modifier = modifier.testTag("payer_${field.apiName}"),
        trailing =
            if (removable) {
                {
                    Icon(
                        PaymentIcons.Close,
                        contentDescription = stringResource(Res.string.payer_remove_address_line),
                        tint = colors.onSurfaceVariant,
                        modifier =
                            Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(role = Role.Button) { state.removeAddressLine(field) }
                                .padding(6.dp),
                    )
                }
            } else {
                null
            },
    )
}

@Composable
private fun CountryCell(
    state: PayerFieldsState,
    spec: PayerFieldSpec,
    highlight: Color?,
    edges: GroupCellEdges,
    modifier: Modifier,
    markMandatory: Boolean,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    val language = LocalSdkLanguage.current
    val code = state.value(spec.field)
    val editable = state.isEditable(spec)
    GroupButtonCell(
        onClick = { state.openCountryPicker(spec.field) },
        highlight = highlight,
        edges = edges,
        enabled = editable,
        modifier = modifier.testTag("payer_${spec.field.apiName}"),
    ) {
        GroupPickerContent(
            label = groupTextOf(spec, markMandatory).label,
            isError = state.visibleErrorOf(spec) != null,
            value =
                if (code.isEmpty()) {
                    null
                } else {
                    {
                        FlagIcon(code, Modifier.padding(end = 4.dp))
                        Text(
                            text = countryDisplayName(code, language),
                            style = typography.input.copy(color = colors.onBackground),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
        )
        if (editable) {
            // In the 28dp slot of "✕" and "+" of the address lines, so that all of them stand on one line.
            Icon(
                PaymentIcons.ChevronDown,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(28.dp).padding(4.dp),
            )
        }
    }
}

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
private fun groupTextOf(
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
private fun PayerTextField(
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

/**
 * Pickers of the payer fields (billing or recipient country, phone code, account and document types of a transfer);
 * draw it in the screen overlay.
 */
@Composable
internal fun CountryPickerSheet(state: PayerFieldsState) {
    val language = LocalSdkLanguage.current
    // ~500 platform name lookups (25–30 ms on iOS): built in the background so the form opens without them.
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

/** A grouped field's state, its error in words for the screen reader. */
@Composable
private fun groupStatusOf(
    state: PayerFieldsState,
    spec: PayerFieldSpec,
): GroupFieldStatus {
    val error = state.visibleErrorOf(spec)
    return GroupFieldStatus(isError = error != null, message = error?.let { stringResource(it.message()) })
}

/** Label of an account or document type code; a code the SDK does not know (e.g. `RTN` from the merchant) as is. */
@Composable
private fun optionLabelOf(code: String): String =
    when (code) {
        "1" -> stringResource(Res.string.transfer_account_type_1)
        "2" -> stringResource(Res.string.transfer_account_type_2)
        "3" -> stringResource(Res.string.transfer_account_type_3)
        "4" -> stringResource(Res.string.transfer_account_type_4)
        "5" -> stringResource(Res.string.transfer_account_type_5)
        "6" -> stringResource(Res.string.transfer_account_type_6)
        "7" -> stringResource(Res.string.transfer_account_type_7)
        "8" -> stringResource(Res.string.transfer_account_type_8)
        "100" -> stringResource(Res.string.transfer_account_type_100)
        "IDTP1" -> stringResource(Res.string.transfer_id_type_1)
        "IDTP2" -> stringResource(Res.string.transfer_id_type_2)
        "IDTP3" -> stringResource(Res.string.transfer_id_type_3)
        "IDTP4" -> stringResource(Res.string.transfer_id_type_4)
        "IDTP5" -> stringResource(Res.string.transfer_id_type_5)
        "IDTP6" -> stringResource(Res.string.transfer_id_type_6)
        "IDTP7" -> stringResource(Res.string.transfer_id_type_7)
        "IDTP8" -> stringResource(Res.string.transfer_id_type_8)
        "IDTP9" -> stringResource(Res.string.transfer_id_type_9)
        "IDTP10" -> stringResource(Res.string.transfer_id_type_10)
        "IDTP11" -> stringResource(Res.string.transfer_id_type_11)
        else -> code
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
                isError = false,
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

/** Phone: a country-code button (flag, `+44`) and the national number, formatted as typed. */
@Composable
private fun PhoneField(
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
                isError = error != null,
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

@Composable
private fun labelOf(spec: PayerFieldSpec): String {
    val base = baseLabelOf(spec.field)
    return if (spec.mandatory) base else stringResource(Res.string.payer_optional, base)
}

@Composable
private fun baseLabelOf(field: PayerField): String =
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

private val PayerField.isAddressLine: Boolean
    get() =
        this == PayerField.BILLING_ADDRESS_LINE1 ||
            this == PayerField.BILLING_ADDRESS_LINE2 ||
            this == PayerField.BILLING_ADDRESS_LINE3

private val PayerField.contentType: TextFieldContentType
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
