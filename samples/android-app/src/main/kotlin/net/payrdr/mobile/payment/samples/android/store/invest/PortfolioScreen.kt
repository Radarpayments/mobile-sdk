package net.payrdr.mobile.payment.samples.android.store.invest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.ArrowUp
import compose.icons.tablericons.Bell
import compose.icons.tablericons.Plus
import compose.icons.tablericons.TrendingUp
import compose.icons.tablericons.Wallet
import net.payrdr.mobile.payment.samples.android.R
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

/**
 * Northwind's home: the portfolio, its free cash and the latest operations. After a top-up the amounts count up to
 * the new values and the free-cash card is outlined while the credit is fresh.
 */
@Composable
internal fun PortfolioScreen(
    account: InvestAccount,
    now: Long,
    onBack: () -> Unit,
    onTopUp: () -> Unit,
    onInvest: () -> Unit,
) {
    var period by rememberSaveable { mutableStateOf(ChartPeriod.MONTH) }
    val justCredited = account.operations.firstOrNull()?.takeIf { it.kind == Operation.Kind.TOP_UP && now - it.atEpochMillis < FRESH_MS }
    Column(
        Modifier
            .fillMaxSize()
            .background(InvestColors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Header(onBack)
        Text(
            stringResource(R.string.invest_portfolio_value),
            modifier = Modifier.padding(top = 18.dp),
            style = plex(13.sp, color = InvestColors.muted),
        )
        Text(eur(countedMinor(account.totalMinor)), style = plex(36.sp, FontWeight.Bold, tabular = true))
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            ChangePill()
            Text(
                stringResource(R.string.invest_change_month),
                modifier = Modifier.padding(start = 8.dp),
                style = plex(13.sp, color = InvestColors.muted),
            )
        }
        PortfolioChart(period, Modifier.padding(top = 10.dp).height(CHART_HEIGHT))
        Spacer(Modifier.height(6.dp))
        PeriodSelector(period) { period = it }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(stringResource(R.string.invest_top_up), onTopUp, Modifier.weight(1f), icon = TablerIcons.Plus)
            OutlineButton(
                stringResource(R.string.invest_invest),
                onInvest,
                Modifier.weight(1f),
                enabled = account.canInvest,
                icon = TablerIcons.TrendingUp,
            )
        }
        FreeCashCard(account.freeCashMinor, justCredited, Modifier.padding(top = 12.dp))
        SectionTitle(stringResource(R.string.invest_my_assets), trailing = account.holdings.size.toString())
        InvestCard(Modifier, highlight = false) {
            account.holdings.forEachIndexed { i, holding -> AssetRow(holding, divider = i > 0) }
        }
        SectionTitle(stringResource(R.string.invest_recent))
        InvestCard {
            account.recent().forEachIndexed { i, op -> OperationRow(op, divider = i > 0) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BackCircle(onBack)
        Box(
            Modifier
                .padding(start = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF166534), Color(0xFF0F766E)))),
            contentAlignment = Alignment.Center,
        ) { Text(NorthwindProfile.INITIALS, style = plex(14.sp, FontWeight.Bold)) }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(stringResource(R.string.invest_account, NorthwindProfile.ACCOUNT_LAST4), style = plex(12.sp, color = InvestColors.muted))
            Text(NorthwindProfile.NAME, style = plex(15.sp, FontWeight.SemiBold))
        }
        // Decorative: the demo has no notifications, so it is not a button.
        Icon(TablerIcons.Bell, contentDescription = null, tint = InvestColors.muted, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ChangePill() {
    val percent = NumberFormat.getPercentInstance(currentLocale()).apply { minimumFractionDigits = 1 }.format(MONTH_CHANGE)
    Text(
        "▲ ${eur(MONTH_CHANGE_MINOR)} · $percent",
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(InvestColors.accentSoft)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        style = plex(13.sp, FontWeight.SemiBold, InvestColors.accent, tabular = true),
    )
}

@Composable
private fun FreeCashCard(
    freeCashMinor: Long,
    justCredited: Operation?,
    modifier: Modifier,
) {
    InvestCard(modifier, highlight = justCredited != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(InvestColors.accentSoft) {
                Icon(TablerIcons.Wallet, contentDescription = null, tint = InvestColors.accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.invest_free_cash), style = plex(13.sp, color = InvestColors.muted))
                if (justCredited != null) {
                    Text(
                        stringResource(R.string.invest_free_cash_just_now, eur(justCredited.amountMinor)),
                        style = plex(12.sp, color = InvestColors.accent, tabular = true),
                    )
                } else {
                    Text(stringResource(R.string.invest_free_cash_sub), style = plex(12.sp, color = InvestColors.muted))
                }
            }
            Text(eur(countedMinor(freeCashMinor)), style = plex(18.sp, FontWeight.Bold, tabular = true))
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    trailing: String? = null,
) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f).semantics { heading() }, style = plex(16.sp, FontWeight.Bold))
        if (trailing != null) Text(trailing, style = plex(13.sp, color = InvestColors.muted))
    }
}

@Composable
private fun AssetRow(
    holding: Holding,
    divider: Boolean,
) {
    ListRow(divider) {
        TickerBadge(holding.ticker)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(assetName(holding.ticker), style = plex(15.sp, FontWeight.SemiBold))
            val quantity =
                NumberFormat.getNumberInstance(currentLocale()).apply {
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
            Text(
                stringResource(R.string.invest_units, quantity.format(holding.quantity()), eur(holding.unitPriceMinor)),
                style = plex(13.sp, color = InvestColors.muted, tabular = true),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(eur(holding.valueMinor), style = plex(15.sp, FontWeight.SemiBold, tabular = true))
            val change = NumberFormat.getPercentInstance(currentLocale()).apply { minimumFractionDigits = 1 }
            Text(
                "+" + change.format(holding.changeBasisPoints / BASIS_POINTS),
                style = plex(13.sp, color = InvestColors.accent, tabular = true),
            )
        }
    }
}

@Composable
private fun OperationRow(
    op: Operation,
    divider: Boolean,
) {
    ListRow(divider) {
        when (op.kind) {
            Operation.Kind.TOP_UP ->
                Badge(InvestColors.accentSoft) {
                    Icon(TablerIcons.ArrowUp, contentDescription = null, tint = InvestColors.accent, modifier = Modifier.size(18.dp))
                }
            Operation.Kind.BUY -> TickerBadge(op.ticker ?: InvestAccount.GLOBAL_ETF)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            val title =
                when (op.kind) {
                    Operation.Kind.TOP_UP -> stringResource(R.string.invest_op_top_up)
                    Operation.Kind.BUY -> stringResource(R.string.invest_op_buy, assetName(op.ticker ?: InvestAccount.GLOBAL_ETF))
                }
            Text(title, style = plex(15.sp, FontWeight.SemiBold))
            Text(operationTime(op.atEpochMillis), style = plex(13.sp, color = InvestColors.muted))
        }
        val sign = if (op.kind == Operation.Kind.TOP_UP) "+" else "−"
        Text(
            sign + eur(op.amountMinor),
            style =
                plex(
                    15.sp,
                    FontWeight.SemiBold,
                    if (op.kind ==
                        Operation.Kind.TOP_UP
                    ) {
                        InvestColors.accent
                    } else {
                        InvestColors.text
                    },
                    tabular = true,
                ),
        )
    }
}

@Composable
private fun ListRow(
    divider: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    Column {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(InvestColors.divider))
        Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) { content() }
    }
}

/** "29 Oct, 13:42" in the app's language. */
@Composable
internal fun operationTime(at: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, currentLocale()).format(Date(at))

private const val FRESH_MS = 60_000L
private const val MONTH_CHANGE = 0.032
private const val MONTH_CHANGE_MINOR = 14920L
private const val BASIS_POINTS = 10_000.0
private val CHART_HEIGHT = 96.dp
