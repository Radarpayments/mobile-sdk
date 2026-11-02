package net.payrdr.mobile.payment.samples.android.store.invest

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronLeft
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import net.payrdr.mobile.payment.samples.android.store.theme.StoreFonts
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Northwind's text: IBM Plex Sans; amounts with tabular figures so they do not jump while counting. */
internal fun plex(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    color: Color = InvestColors.text,
    tabular: Boolean = false,
) = TextStyle(
    fontFamily = StoreFonts.plex,
    fontWeight = weight,
    fontSize = size,
    color = color,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

@Composable
internal fun currentLocale(): Locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

@Composable
internal fun eur(minor: Long): String = formatEur(minor, currentLocale())

/** Whole euros without the cents, for the preset chips ("€250"). */
@Composable
internal fun eurWhole(minor: Long): String =
    NumberFormat
        .getCurrencyInstance(currentLocale())
        .apply {
            currency = Currency.getInstance("EUR")
            maximumFractionDigits = 0
        }.format(minor / CENTS)

/** Whether the system asks for no animations (developer options or accessibility). */
@Composable
internal fun reducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}

/** [target] counted up (or down) from the value shown before, so a credit is seen arriving. */
@Composable
internal fun countedMinor(target: Long): Long {
    val reduced = reducedMotion()
    val value = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) {
        if (reduced) value.snapTo(target.toFloat()) else value.animateTo(target.toFloat(), tween(COUNT_MS, easing = FastOutSlowInEasing))
    }
    return value.value.toLong()
}

@Composable
internal fun InvestCard(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(InvestColors.surface)
            .border(
                BorderStroke(1.dp, if (highlight) InvestColors.accent.copy(alpha = 0.7f) else InvestColors.divider),
                RoundedCornerShape(18.dp),
            ).padding(horizontal = 16.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
internal fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = RoundedCornerShape(14.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = InvestColors.accent,
                contentColor = InvestColors.onAccent,
                disabledContainerColor = InvestColors.accent.copy(alpha = if (busy) 1f else 0.35f),
                disabledContentColor = InvestColors.onAccent.copy(alpha = if (busy) 1f else 0.7f),
            ),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        ButtonContent(label, icon, busy)
    }
}

@Composable
internal fun OutlineButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, InvestColors.fieldBorder.copy(alpha = if (enabled) 1f else 0.5f)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = InvestColors.text,
                disabledContentColor = InvestColors.muted,
            ),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        ButtonContent(label, icon, busy = false)
    }
}

@Composable
private fun RowScope.ButtonContent(
    label: String,
    icon: ImageVector?,
    busy: Boolean,
) {
    if (busy) {
        CircularProgressIndicator(color = InvestColors.onAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.padding(start = 10.dp), style = plex(15.sp, FontWeight.Bold, InvestColors.onAccent))
        return
    }
    if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 2.dp))
    Text(
        label,
        modifier = Modifier.padding(start = if (icon != null) 6.dp else 0.dp),
        // The button sets the content colour (enabled or not); the text follows it.
        style = plex(15.sp, FontWeight.Bold, LocalContentColor.current),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Round back button of the inner screens. */
@Composable
internal fun BackCircle(onBack: () -> Unit) {
    IconButton(
        onClick = onBack,
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(InvestColors.surface)
                .border(1.dp, InvestColors.divider, CircleShape),
    ) {
        Icon(TablerIcons.ChevronLeft, contentDescription = stringResource(R.string.store_back), tint = InvestColors.text)
    }
}

/** Title bar of an inner screen: back on the start side, the title centred. */
@Composable
internal fun InnerTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxWidth()) { BackCircle(onBack) }
        Text(title, style = plex(17.sp, FontWeight.SemiBold))
    }
}

/** The square badge of a fund or an operation. */
@Composable
internal fun Badge(
    background: Color,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
internal fun TickerBadge(ticker: String) {
    val (bg, fg) =
        if (ticker ==
            InvestAccount.GLOBAL_ETF
        ) {
            InvestColors.glb to InvestColors.onGlb
        } else {
            InvestColors.tch to InvestColors.onTch
        }
    Badge(bg) { Text(ticker, style = plex(12.sp, FontWeight.Bold, fg)) }
}

@Composable
internal fun assetName(ticker: String): String =
    stringResource(if (ticker == InvestAccount.GLOBAL_ETF) R.string.invest_asset_glb else R.string.invest_asset_tch)

/** A label–value line of a details card. */
@Composable
internal fun DetailRow(
    label: String,
    value: @Composable () -> Unit,
    divider: Boolean = true,
) {
    Column {
        if (divider) Box(Modifier.fillMaxWidth().heightIn(min = 1.dp).background(InvestColors.divider))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = plex(14.sp, color = InvestColors.muted))
            value()
        }
    }
}

private const val COUNT_MS = 600
private const val CENTS = 100
