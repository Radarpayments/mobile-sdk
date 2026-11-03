package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PaymentReceipt
import net.payrdr.mobile.payment.sdk.payment.ui.LocalSdkLanguage
import net.payrdr.mobile.payment.sdk.payment.ui.components.AnnounceScreenChange
import net.payrdr.mobile.payment.sdk.payment.ui.components.ButtonKind
import net.payrdr.mobile.payment.sdk.payment.ui.components.CardNetworkIcon
import net.payrdr.mobile.payment.sdk.payment.ui.components.CheckoutButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.LocalSaveCardOnly
import net.payrdr.mobile.payment.sdk.payment.ui.components.ResultMark
import net.payrdr.mobile.payment.sdk.payment.ui.components.SIDE_PADDING
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberReduceMotion
import net.payrdr.mobile.payment.sdk.payment.ui.components.rememberResultHaptics
import net.payrdr.mobile.payment.sdk.payment.ui.motion.cascadeIn
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.errorSoft
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardFormatters
import net.payrdr.mobile.payment.sdk.payment.ui.util.CardNetworkResolver
import net.payrdr.mobile.payment.sdk.payment.ui.util.GatewayDateTime
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.payment.ui.util.receiptDateText
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.receipt_card
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.receipt_date
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.receipt_order
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.receipt_order_number
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_card_saved_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_close
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_done
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_failure_hint
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_failure_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_session_expired
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_success_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_try_again
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Terminal screen shown when `PaymentState` transitions to `Completed` (turnkey checkout shows it
 * only with `SdkPaymentConfig.showResultScreen`).
 *  - success: check mark, amount, a receipt (card, date, order) and Done. The approval code and RRN are not shown:
 *    they stay in [PaymentReceipt] for the merchant.
 *  - failure: cross, amount, the bank's reason, the technical error as a small code chip, and
 *    "Pay with another card" (if supported) + Close.
 *
 * @param onClose invoked when the user taps Done / Close; also the navigation-up handler
 * @param onRetry optional "pay with another card" handler — when null, the button is hidden
 * @param receipt final order details; null hides the receipt
 * @param order amount / merchant / order number for the receipt
 */
@Composable
public fun ResultScreen(
    result: CheckoutResult,
    onClose: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    receipt: PaymentReceipt? = null,
    order: OrderSummary? = null,
) {
    ResultContent(
        result = result,
        onClose = onClose,
        onRetry = onRetry,
        receipt = receipt,
        order = order,
        fillHeight = true,
        modifier = modifier.fillMaxSize().background(LocalPaymentColors.current.background),
    )
}

/**
 * The result itself. [fillHeight]: centred in the whole height, as the full-screen [ResultScreen]; otherwise as tall as
 * its content (the checkout sheet), the details scrolling above the buttons once they no longer fit.
 */
@Composable
internal fun ResultContent(
    result: CheckoutResult,
    onClose: () -> Unit,
    onRetry: (() -> Unit)?,
    receipt: PaymentReceipt?,
    order: OrderSummary?,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    val amount = receipt?.formattedAmount ?: order?.formattedAmount
    val reduceMotion = rememberReduceMotion()
    val haptics = rememberResultHaptics()
    val success = result is CheckoutResult.Completed
    // A payment going through is rare enough to deserve the receipt coming in line by line.
    val cascade = success && !reduceMotion
    AnnounceScreenChange()

    Column(
        modifier =
            modifier
                // In the sheet the sheet keeps the space under the buttons, so they stand where its other steps' buttons do.
                .padding(start = SIDE_PADDING, end = SIDE_PADDING, top = 18.dp, bottom = if (fillHeight) 18.dp else 0.dp)
                .testTag("ResultScreen"),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = fillHeight).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            ResultMark(success = success, reduceMotion = reduceMotion, onMarkStart = { haptics(success) })
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(resultTitle(success, LocalSaveCardOnly.current)),
                style = typography.headline.copy(color = colors.onBackground),
                textAlign = TextAlign.Center,
                // Read out as soon as it shows: the outcome of the payment is the one thing the buyer waits for.
                modifier =
                    Modifier.cascadeIn(0, cascade).semantics {
                        heading()
                        liveRegion = LiveRegionMode.Assertive
                    },
            )
            if (amount != null) {
                Text(
                    text = ltr(amount),
                    style = typography.amount.copy(fontSize = typography.amount.fontSize * 0.93f, color = colors.onBackground),
                    modifier = Modifier.cascadeIn(1, cascade),
                )
            }
            if (success) {
                if (receipt != null) {
                    Spacer(Modifier.height(10.dp))
                    Receipt(receipt = receipt, order = order, cascade = cascade)
                }
            } else {
                val error = (result as? CheckoutResult.Failed)?.error
                val description = receipt?.actionCodeDescription?.takeIf { it.isNotBlank() }
                Text(
                    text = description ?: stringResource(failureHint(error)),
                    style = typography.body.copy(color = colors.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp).widthIn(max = 300.dp),
                )
                val code = failureChip(description = description, error = error)
                if (code != null) {
                    Text(
                        text = code,
                        style = typography.caption.tabular().copy(color = colors.error),
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .padding(top = 6.dp)
                                .background(colors.errorSoft, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }
        if (!fillHeight) Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (success) {
                CheckoutButton(text = stringResource(Res.string.result_done), onClick = onClose, primary = true)
            } else {
                if (onRetry != null) {
                    CheckoutButton(text = stringResource(Res.string.result_try_again), onClick = onRetry, primary = true)
                }
                CheckoutButton(
                    text = stringResource(Res.string.result_close),
                    onClick = onClose,
                    kind = if (onRetry != null) ButtonKind.Secondary else ButtonKind.Primary,
                    primary = onRetry == null,
                )
            }
        }
    }
}

/** The headline: an order that only saves a card says "Card saved" once it did; a failure reads the same either way. */
internal fun resultTitle(
    success: Boolean,
    saveCardOnly: Boolean,
): StringResource =
    when {
        success && saveCardOnly -> Res.string.result_card_saved_title
        success -> Res.string.result_success_title
        else -> Res.string.result_failure_title
    }

/** What to say under the amount when the gateway gave no reason: another card does not help once the session expired. */
internal fun failureHint(error: SdkException?): StringResource =
    if (error is SdkSessionExpiredException) Res.string.result_session_expired else Res.string.result_failure_hint

/**
 * The chip for [error]: only what came from the gateway — a decline in its words, which are the buyer's, or the code of
 * an error it answered with. A failure of the SDK's own (no network, a session not found) gets none: the buyer can do
 * nothing with it, the text above already says what to do, and the app gets its type in the result. The SDK's messages
 * are for developers: English, with ids and hosts in them.
 */
internal fun failureChip(
    description: String?,
    error: SdkException?,
): String? =
    when (error) {
        is SdkDeclinedException -> failureChipText(description = description, error = error.message)
        is SdkPaymentApiException -> error.errorCode?.takeIf { it.isNotBlank() } ?: error.httpStatus?.let { "HTTP $it" }
        else -> null
    }

/**
 * The gateway's decline reason for the chip under the description, or null when there is none or it only repeats
 * [description]: a bank's decline comes as both, and saying it twice reads as two different problems.
 */
internal fun failureChipText(
    description: String?,
    error: String?,
): String? {
    val code = error?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return code.takeUnless { description != null && it.equals(description.trim(), ignoreCase = true) }
}

@Composable
private fun Receipt(
    receipt: PaymentReceipt,
    order: OrderSummary?,
    cascade: Boolean,
) {
    val language = LocalSdkLanguage.current
    val pan = receipt.panMasked?.takeIf { it.any(Char::isDigit) }
    val date = receipt.paymentDate?.let { raw -> GatewayDateTime.parse(raw)?.let { receiptDateText(it, language) } ?: raw }
    val rows =
        buildList<@Composable () -> Unit> {
            if (pan != null) {
                add {
                    ReceiptRow(stringResource(Res.string.receipt_card), CardFormatters.shortMask(pan)) {
                        CardNetworkIcon(CardNetworkResolver.resolveSaved(paymentSystem = "", maskedPan = pan))
                    }
                }
            }
            if (date != null) add { ReceiptRow(stringResource(Res.string.receipt_date), date) }
            order?.orderNumber?.let { number ->
                add { ReceiptRow(stringResource(Res.string.receipt_order), stringResource(Res.string.receipt_order_number, number)) }
            }
        }
    if (rows.isEmpty()) return
    // A list with hairlines between its lines and no frame: the frame made the details outweigh the result above them.
    Column(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            // After the title (0) and the amount (1).
            Column(Modifier.cascadeIn(index + 2, cascade)) {
                if (index > 0) HorizontalDivider(color = LocalPaymentColors.current.outline)
                row()
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = typography.label.copy(color = colors.onSurfaceVariant, fontWeight = typography.body.fontWeight))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Text(
                text = ltr(value),
                style = typography.label.tabular().copy(color = colors.onBackground),
                textAlign = TextAlign.End,
            )
        }
    }
}
