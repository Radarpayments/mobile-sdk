package net.payrdr.mobile.payment.sdk.payment.ui.payer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldGroupFrame
import net.payrdr.mobile.payment.sdk.payment.ui.components.FieldGroupHeader
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupButtonCell
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupCellEdges
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupPickerContent
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupRow
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupRowDivider
import net.payrdr.mobile.payment.sdk.payment.ui.components.GroupTextCell
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.keepVisibleWhileFocused
import net.payrdr.mobile.payment.sdk.payment.ui.screens.message
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.payer_optional_note
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_1
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_100
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_2
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_3
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_4
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_5
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_6
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_7
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_account_type_8
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
import org.jetbrains.compose.resources.stringResource

/**
 * The sender or the recipient of a transfer as one framed group, a field per row: text, a date typed `DD.MM.YYYY`, a
 * country, or a code picked from a list (account and document types).
 */
@Composable
internal fun TransferGroup(
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

/** Label of an account or document type code; a code the SDK does not know (e.g. `RTN` from the merchant) as is. */
@Composable
internal fun optionLabelOf(code: String): String =
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
