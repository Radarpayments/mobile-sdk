package net.payrdr.mobile.payment.samples.android.store.invest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.Check
import net.payrdr.mobile.payment.samples.android.R

/** The transfer went through: what was credited, to where, and the order to find it by at the gateway. */
@Composable
internal fun TopUpDoneScreen(
    done: TopUpStatus.Done,
    canInvest: Boolean,
    onInvest: () -> Unit,
    onPortfolio: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(InvestColors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 64.dp)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(InvestColors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(60.dp).clip(CircleShape).background(InvestColors.accent), contentAlignment = Alignment.Center) {
                    Icon(TablerIcons.Check, contentDescription = null, tint = InvestColors.onAccent, modifier = Modifier.size(30.dp))
                }
            }
            Text(
                "+" + eur(done.amountMinor),
                modifier = Modifier.padding(top = 22.dp),
                style = plex(40.sp, FontWeight.Bold, tabular = true),
            )
            Text(
                stringResource(R.string.done_credited, NorthwindProfile.ACCOUNT_LAST4),
                textAlign = TextAlign.Center,
                style = plex(15.sp, color = InvestColors.muted),
            )
            InvestCard(Modifier.padding(top = 28.dp)) {
                DetailRow(stringResource(R.string.done_operation), {
                    Text(stringResource(R.string.done_operation_value), style = plex(14.sp))
                }, divider = false)
                DetailRow(stringResource(R.string.done_free_cash), {
                    Text(
                        "${eur(done.freeCashBefore)} → ${eur(done.freeCashAfter)}",
                        style = plex(14.sp, FontWeight.SemiBold, InvestColors.accent, tabular = true),
                    )
                })
                DetailRow(stringResource(R.string.done_order), {
                    Text(
                        middleEllipsis(done.orderId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = plex(14.sp, tabular = true),
                    )
                })
                DetailRow(stringResource(R.string.done_time), { Text(operationTime(done.at), style = plex(14.sp)) })
            }
        }
        Column(Modifier.padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                stringResource(R.string.done_invest, eur(done.amountMinor)),
                onInvest,
                Modifier.fillMaxWidth(),
                enabled = canInvest,
            )
            OutlineButton(stringResource(R.string.done_to_portfolio), onPortfolio, Modifier.fillMaxWidth())
        }
    }
}

/** "2cd7e4de…2ef4095f": both ends of an order id, which is what people compare. */
internal fun middleEllipsis(id: String): String = if (id.length <= ID_SHOWN) id else id.take(ID_END) + "…" + id.takeLast(ID_END)

private const val ID_SHOWN = 20
private const val ID_END = 8
