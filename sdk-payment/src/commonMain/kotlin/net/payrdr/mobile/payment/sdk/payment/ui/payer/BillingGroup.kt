package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.LocalSdkLanguage
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldGroupFrame
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldGroupHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupButtonCell
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupCellDivider
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupCellEdges
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupPickerContent
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupRow
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupRowDivider
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupTextCell
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.TextAction
import net.payrdr.mobile.payment.sdk.payment.ui.components.keepVisibleWhileFocused
import net.payrdr.mobile.payment.sdk.payment.ui.screens.message
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.util.FlagIcon
import net.payrdr.mobile.payment.sdk.payment.ui.util.countryDisplayName
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_add_address_line
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_billing_add
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_billing_help
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_billing_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_optional_note
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_remove_address_line
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_summary_collapse
import org.jetbrains.compose.resources.stringResource

/**
 * Optional billing fields kept out of the way: a row to add them, the group with "Hide", or the entered
 * address in one line with "Edit". A wrong value opens the group (Pay marks every field), and "Hide" closes it
 * only once the values pass — hiding one would send it unseen.
 */
@Composable
internal fun CollapsibleBillingGroup(
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
internal fun BillingGroup(
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
internal fun CountryCell(
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
