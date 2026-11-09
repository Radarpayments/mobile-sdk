package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetwork
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_card_details_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_cvv_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_expiry_label
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_expiry_placeholder
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_holder_placeholder
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_pan_label
import org.jetbrains.compose.resources.stringResource

/**
 * Card number, expiry with CVC, and (when [holder] is not null) the name on the card — one frame split by
 * hairlines, hints inside the inputs instead of labels. A focused or failing input outlines itself over the
 * shared frame; the first error's text goes under the group.
 *
 * @param holder name on the card; null hides that row
 * @param message error text shown under the group, null for none
 * @param labelActions actions at the right of the group label (Scan)
 */
@Composable
internal fun CardDetailsGroup(
    pan: String,
    onPanChange: (String) -> Unit,
    expiry: String,
    onExpiryChange: (String) -> Unit,
    cvc: String,
    onCvcChange: (String) -> Unit,
    network: CardNetwork,
    panStatus: GroupFieldStatus,
    expiryStatus: GroupFieldStatus,
    cvcStatus: GroupFieldStatus,
    message: String?,
    modifier: Modifier = Modifier,
    holder: String? = null,
    onHolderChange: (String) -> Unit = {},
    onHolderFocusChange: (Boolean) -> Unit = {},
    holderStatus: GroupFieldStatus = GroupFieldStatus(),
    labelActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    var focusedCell by remember { mutableStateOf<GroupCell?>(null) }
    val rows = if (holder != null) 3 else 2

    fun focus(
        cell: GroupCell,
        focused: Boolean,
    ) {
        if (focused) {
            focusedCell = cell
        } else if (focusedCell == cell) {
            focusedCell = null
        }
    }

    fun highlightOf(
        cell: GroupCell,
        status: GroupFieldStatus,
    ): Color? =
        when {
            status.isError -> colors.error
            focusedCell == cell -> colors.primary
            else -> null
        }

    Column(
        modifier = modifier.keepVisibleWhileFocused(focusedCell != null),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FieldGroupHeader(title = stringResource(Res.string.card_form_card_details_label), trailing = labelActions)
        FieldGroupFrame {
            val panHighlight = highlightOf(GroupCell.PAN, panStatus)
            GroupRow(highlighted = panHighlight != null) {
                GroupTextCell(
                    value = CardFormatters.formatPan(pan),
                    onValueChange = { text ->
                        val digits = CardFormatters.digitsOnly(text)
                        onPanChange(digits.take(CardFormatters.maxPanLength(CardNetworkResolver.resolve(digits))))
                    },
                    placeholder = stringResource(Res.string.card_form_pan_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    contentType = TextFieldContentType.CREDIT_CARD_NUMBER,
                    status = panStatus,
                    highlight = panHighlight,
                    edges = GroupCellEdges.of(row = 0, rows = rows),
                    onFocusChange = { focus(GroupCell.PAN, it) },
                    modifier = Modifier.fillMaxWidth(),
                    trailing = if (network != CardNetwork.UNKNOWN) ({ CardNetworkIcon(network) }) else null,
                )
            }
            GroupRowDivider()
            val expiryHighlight = highlightOf(GroupCell.EXPIRY, expiryStatus)
            val cvcHighlight = highlightOf(GroupCell.CVC, cvcStatus)
            GroupRow(highlighted = expiryHighlight != null || cvcHighlight != null) {
                GroupTextCell(
                    value = CardFormatters.formatExpiry(expiry),
                    onValueChange = { text ->
                        var next = CardFormatters.digitsOnly(text).take(4)
                        if (next.length == 1 && next[0] > '1') next = "0$next"
                        onExpiryChange(next)
                    },
                    placeholder = stringResource(Res.string.card_form_expiry_placeholder),
                    label = stringResource(Res.string.card_form_expiry_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    contentType = TextFieldContentType.CREDIT_CARD_EXPIRATION,
                    status = expiryStatus,
                    highlight = expiryHighlight,
                    edges = GroupCellEdges.of(row = 1, rows = rows, column = 0, columns = 2),
                    onFocusChange = { focus(GroupCell.EXPIRY, it) },
                    modifier = Modifier.weight(1f),
                )
                GroupCellDivider()
                val maxCvc = CardFormatters.cvvLength(network)
                GroupTextCell(
                    value = CardFormatters.digitsOnly(cvc).take(maxCvc),
                    onValueChange = { text -> onCvcChange(CardFormatters.digitsOnly(text).take(maxCvc)) },
                    placeholder = stringResource(Res.string.card_form_cvv_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    contentType = TextFieldContentType.CREDIT_CARD_SECURITY_CODE,
                    status = cvcStatus,
                    highlight = cvcHighlight,
                    edges = GroupCellEdges.of(row = 1, rows = rows, column = 1, columns = 2),
                    onFocusChange = { focus(GroupCell.CVC, it) },
                    modifier = Modifier.weight(1f),
                    trailing = { CvcHintButton(network) },
                )
            }
            if (holder != null) {
                GroupRowDivider()
                val holderHighlight = highlightOf(GroupCell.HOLDER, holderStatus)
                GroupRow(highlighted = holderHighlight != null) {
                    GroupTextCell(
                        value = holder,
                        // Printed names are upper case and at most 26 characters (ISO/IEC 7813).
                        onValueChange = { text -> onHolderChange(text.uppercase().trimStart().take(MAX_HOLDER_LENGTH)) },
                        placeholder = stringResource(Res.string.card_form_holder_placeholder),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                capitalization = KeyboardCapitalization.Characters,
                                autoCorrectEnabled = false,
                            ),
                        contentType = TextFieldContentType.CREDIT_CARD_HOLDER,
                        status = holderStatus,
                        highlight = holderHighlight,
                        edges = GroupCellEdges.of(row = 2, rows = rows),
                        onFocusChange = {
                            focus(GroupCell.HOLDER, it)
                            onHolderFocusChange(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (!message.isNullOrEmpty()) {
            // Read out as it appears; each field also carries its own error for the screen reader.
            Text(
                text = message,
                style = typography.caption.copy(color = colors.error),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private enum class GroupCell { PAN, EXPIRY, CVC, HOLDER }

private const val MAX_HOLDER_LENGTH = 26
