package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_pay
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_save_card_only
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.card_form_transfer
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.pay_amount
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.transfer_amount
import org.jetbrains.compose.resources.stringResource

/**
 * Primary Pay button: accent fill, label such as `Pay €49.90`, a lock before it. Renders a progress
 * spinner in place of the label when [loading] is true.
 *
 * @param label button text, typically `stringResource(Res.string.pay_amount, amount)` in the caller
 * @param enabled whether clicking is allowed; disabled buttons are drawn at reduced opacity
 * @param loading render a progress spinner in place of the label
 * @param onClick click handler; only fired when [enabled] and not [loading]
 * @param showLock the lock before the label — the one sign of a protected payment the checkout shows
 */
@Composable
internal fun PayButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
    showLock: Boolean = true,
) {
    CheckoutButton(
        text = label,
        onClick = onClick,
        icon = PaymentIcons.LockBold.takeIf { showLock },
        enabled = enabled,
        loading = loading,
        modifier = modifier,
        primary = true,
    )
}

/**
 * The checkout pays a transfer (an AFT order, see `PaymentState.CardFormShown.transfer`): its buttons say "Transfer".
 * Provided by the checkout; a standalone screen takes it as a parameter.
 */
internal val LocalTransfer = compositionLocalOf { false }

/**
 * The checkout only saves a card (a `VERIFY` order, see `PaymentState.CardFormShown.saveCardOnly`): nothing is paid,
 * its button says "Save card" and its result "Card saved". Provided by the checkout; a standalone screen takes it as a
 * parameter.
 */
internal val LocalSaveCardOnly = compositionLocalOf { false }

/**
 * Label of the main button: `Pay €49.90` (or `Transfer €49.90`), or just the verb before the amount is known; "Save
 * card" when the order only saves one.
 */
@Composable
internal fun payLabel(
    order: OrderSummary?,
    transfer: Boolean = LocalTransfer.current,
    saveCardOnly: Boolean = LocalSaveCardOnly.current,
): String =
    when {
        saveCardOnly -> stringResource(Res.string.card_form_save_card_only)
        else ->
            order?.formattedAmount?.let { stringResource(if (transfer) Res.string.transfer_amount else Res.string.pay_amount, ltr(it)) }
                ?: stringResource(if (transfer) Res.string.card_form_transfer else Res.string.card_form_pay)
    }
