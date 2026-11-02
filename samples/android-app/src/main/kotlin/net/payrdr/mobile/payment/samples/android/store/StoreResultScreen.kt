package net.payrdr.mobile.payment.samples.android.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import compose.icons.tablericons.Clock
import compose.icons.tablericons.X
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import net.payrdr.mobile.payment.samples.android.store.theme.CoverTheme
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle

/**
 * The store's own screen after the sheet closes: the merchant app, not the SDK, decides what comes
 * next. [headline] replaces the default title area (the coffee shop shows its order number).
 */
@Composable
fun StoreResultScreen(
    cover: CoverId,
    receipt: StoreViewModel.Receipt,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    headline: (@Composable () -> Unit)? = null,
) {
    val style = coverStyle(cover)
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()
    val success = receipt.outcome as? CheckoutOutcome.Success
    val unconfirmed = receipt.outcome as? CheckoutOutcome.Unconfirmed
    CoverTheme(cover) {
        Column(
            Modifier
                .fillMaxSize()
                .background(style.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.6f))
            if (success != null) {
                headline?.invoke() ?: run {
                    ResultMark(ok = true, tint = style.accent)
                    Title(successTitle(cover), style.headingFont, style.text)
                }
                Body(stringResource(successBody(cover)), style.bodyFont, style.muted)
                Column(
                    Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                        .border(1.dp, style.border, RoundedCornerShape(style.radius))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InfoRow(
                        stringResource(R.string.result_amount),
                        formatEur(receipt.amountMinor, locale),
                        style.bodyFont,
                        style.muted,
                        style.text,
                    )
                    InfoRow(stringResource(R.string.result_payment_id), success.paymentId, FontFamily.Monospace, style.muted, style.text)
                }
            } else if (unconfirmed != null) {
                Box(Modifier.size(72.dp).border(1.5.dp, style.muted, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(TablerIcons.Clock, contentDescription = null, tint = style.muted, modifier = Modifier.size(32.dp))
                }
                Title(R.string.result_unconfirmed_title, style.headingFont, style.text)
                Body(stringResource(R.string.result_unconfirmed_body), style.bodyFont, style.muted)
            } else {
                ResultMark(ok = false, tint = Color(0xFFDC2626))
                Title(R.string.result_declined_title, style.headingFont, style.text)
                Body(stringResource(R.string.result_declined_body), style.bodyFont, style.muted)
                // The gateway's own words, as they came: useful when showing the demo, secondary for a buyer.
                (receipt.outcome as? CheckoutOutcome.Declined)?.message?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(top = 12.dp),
                        style = TextStyle(fontFamily = style.bodyFont, fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center),
                        color = style.muted,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            // Paying again while the first payment may have gone through could charge the buyer twice.
            if (success == null && unconfirmed == null) {
                PayButton(cover, stringResource(R.string.result_try_again), busy = false, enabled = true, onClick = onRetry)
                Spacer(Modifier.size(10.dp))
            }
            OutlinedButton(
                onClick = onDone,
                shape = if (style.pillButtons) RoundedCornerShape(percent = 50) else RoundedCornerShape(style.radius),
                border = BorderStroke(1.dp, style.border),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(
                    stringResource(if (success != null) R.string.result_done else R.string.result_back_to_store),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = style.text,
                )
            }
        }
    }
}

@Composable
private fun ResultMark(
    ok: Boolean,
    tint: Color,
) {
    Box(
        Modifier.size(72.dp).border(1.5.dp, tint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (ok) TablerIcons.Check else TablerIcons.X, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun Title(
    text: Int,
    font: FontFamily,
    color: Color,
) {
    Text(
        stringResource(text),
        modifier = Modifier.padding(top = 18.dp),
        style =
            TextStyle(
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
            ),
        color = color,
    )
}

@Composable
private fun Body(
    text: String,
    font: FontFamily,
    color: Color,
) {
    Text(
        text,
        modifier = Modifier.padding(top = 8.dp),
        style = TextStyle(fontFamily = font, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center),
        color = color,
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueFont: FontFamily,
    labelColor: Color,
    valueColor: Color,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = TextStyle(fontSize = 13.sp), color = labelColor)
        Text(value, style = TextStyle(fontFamily = valueFont, fontSize = 13.sp, fontWeight = FontWeight.Medium), color = valueColor)
    }
}

private fun successTitle(cover: CoverId) =
    when (cover) {
        CoverId.FURNITURE -> R.string.result_success_furniture
        CoverId.COFFEE -> R.string.result_success_coffee
        CoverId.TICKETS -> R.string.result_success_tickets
        CoverId.INVEST -> R.string.invest_op_top_up
    }

private fun successBody(cover: CoverId) =
    when (cover) {
        CoverId.FURNITURE -> R.string.result_success_furniture_body
        CoverId.COFFEE -> R.string.result_success_coffee_body
        CoverId.TICKETS -> R.string.result_success_tickets_body
        CoverId.INVEST -> R.string.done_operation_value
    }
