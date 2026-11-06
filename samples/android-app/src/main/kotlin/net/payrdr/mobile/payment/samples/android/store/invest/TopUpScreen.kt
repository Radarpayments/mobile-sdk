package net.payrdr.mobile.payment.samples.android.store.invest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.AlertCircle
import compose.icons.tablericons.CircleCheck
import compose.icons.tablericons.CreditCard
import compose.icons.tablericons.TrendingUp
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutState
import java.text.DecimalFormatSymbols

internal const val ADD_CARD_TAG = "topup_add_card"

/**
 * The amount to top up the account with. "Continue" registers the transfer ([onContinue]) and the SDK sheet takes
 * it from there; "Add card" ([onAddCard]) saves a card for later top-ups without moving money ([addingCard] while it
 * runs); [footer] is the demo panel row and a registration error, if any.
 */
@Composable
internal fun TopUpScreen(
    vm: InvestViewModel,
    checkout: CheckoutState,
    addingCard: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onAddCard: () -> Unit,
    footer: @Composable () -> Unit,
) {
    val account by vm.account.collectAsState()
    val amount by vm.amount.collectAsState()
    val status by vm.status.collectAsState()
    val busy = checkout == CheckoutState.Registering || checkout == CheckoutState.InSheet
    Column(
        Modifier
            .fillMaxSize()
            .background(InvestColors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            InnerTopBar(stringResource(R.string.topup_title), onBack)
            StatusBanner(status)
            DestinationCard(account.freeCashMinor, Modifier.padding(top = 12.dp))
            OutlineButton(
                stringResource(if (busy && addingCard) R.string.topup_adding_card else R.string.topup_add_card),
                onAddCard,
                Modifier.fillMaxWidth().padding(top = 10.dp).testTag(ADD_CARD_TAG),
                enabled = !busy,
                icon = TablerIcons.CreditCard,
            )
            Text(
                stringResource(R.string.topup_amount),
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                textAlign = TextAlign.Center,
                style = plex(13.sp, color = InvestColors.muted),
            )
            AmountField(amount, vm::edit)
            Text(
                stringResource(R.string.topup_limits, eur(TopUpAmount.MIN), eur(TopUpAmount.MAX)),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style =
                    plex(
                        13.sp,
                        color =
                            if (amount.valid ||
                                amount.text.isEmpty()
                            ) {
                                InvestColors.muted
                            } else {
                                InvestColors.error
                            },
                        tabular = true,
                    ),
            )
            Presets(amount, vm::preset, Modifier.padding(top = 16.dp))
        }
        Column(Modifier.padding(top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            footer()
            val label =
                when {
                    busy && !addingCard -> stringResource(R.string.topup_creating)
                    status is TopUpStatus.Declined -> stringResource(R.string.topup_try_again)
                    else -> stringResource(R.string.topup_continue, amount.minor?.let { eur(it) } ?: "—")
                }
            PrimaryButton(
                label,
                onContinue,
                Modifier.fillMaxWidth(),
                enabled = status.canContinue(amount) && !busy,
                busy = busy && !addingCard,
            )
        }
    }
}

@Composable
private fun StatusBanner(status: TopUpStatus) {
    val (titleRes, bodyRes) =
        when (status) {
            is TopUpStatus.Declined -> R.string.topup_failed_title to R.string.topup_failed_body
            TopUpStatus.Unconfirmed -> R.string.topup_unknown_title to R.string.topup_unknown_body
            TopUpStatus.CardAdded -> R.string.topup_card_added_title to R.string.topup_card_added_body
            is TopUpStatus.CardDeclined -> R.string.topup_card_failed_title to R.string.topup_card_failed_body
            TopUpStatus.CardUnconfirmed -> R.string.topup_unknown_title to R.string.topup_card_unknown_body
            else -> return
        }
    val title = stringResource(titleRes)
    val body = stringResource(bodyRes)
    val tint = if (status == TopUpStatus.CardAdded) InvestColors.accent else InvestColors.error
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (status == TopUpStatus.CardAdded) InvestColors.accentSoft else InvestColors.errorSoft)
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(
            if (status == TopUpStatus.CardAdded) TablerIcons.CircleCheck else TablerIcons.AlertCircle,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.padding(start = 10.dp)) {
            Text(title, style = plex(15.sp, FontWeight.SemiBold))
            Text(body, style = plex(13.sp, color = InvestColors.muted))
            ((status as? TopUpStatus.Declined)?.message ?: (status as? TopUpStatus.CardDeclined)?.message)?.let {
                Text(
                    stringResource(R.string.topup_failed_detail, it),
                    modifier = Modifier.padding(top = 6.dp),
                    style = plex(12.sp, color = InvestColors.muted),
                )
            }
        }
    }
}

@Composable
private fun DestinationCard(
    freeCashMinor: Long,
    modifier: Modifier,
) {
    InvestCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(InvestColors.accentSoft) {
                Icon(TablerIcons.TrendingUp, contentDescription = null, tint = InvestColors.accent, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.topup_to), style = plex(12.sp, color = InvestColors.muted))
                Text(stringResource(R.string.invest_account, NorthwindProfile.ACCOUNT_LAST4), style = plex(15.sp, FontWeight.SemiBold))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.topup_available), style = plex(12.sp, color = InvestColors.muted))
                Text(eur(freeCashMinor), style = plex(15.sp, FontWeight.SemiBold, tabular = true))
            }
        }
    }
}

/**
 * The amount, typed on the system keyboard (decimal digits) with the locale's separator. Focused when the screen
 * opens, so the keyboard comes up at once; "Done" hides it.
 */
@Composable
private fun AmountField(
    amount: TopUpAmount,
    onEdit: (String) -> Unit,
) {
    val separator = DecimalFormatSymbols.getInstance(currentLocale()).decimalSeparator
    val shown = amount.text.replace('.', separator)
    var field by remember { mutableStateOf(TextFieldValue(shown, TextRange(shown.length))) }
    // A preset chip, or a character the format dropped: show what the amount really is, caret at the end.
    LaunchedEffect(shown) { if (field.text != shown) field = TextFieldValue(shown, TextRange(shown.length)) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val keyboard = LocalSoftwareKeyboardController.current
    val style = plex(52.sp, FontWeight.Bold, tabular = true)
    BasicTextField(
        value = field,
        onValueChange = {
            // What the amount kept, not what was typed: a third decimal, a seventh digit or a second separator goes.
            val kept = TopUpAmount.parse(it.text).text.replace('.', separator)
            field = if (kept == it.text) it else TextFieldValue(kept, TextRange(kept.length))
            onEdit(it.text)
        },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        textStyle = style,
        singleLine = true,
        cursorBrush = SolidColor(InvestColors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("€", style = style)
                Box(Modifier.width(IntrinsicSize.Max)) {
                    if (field.text.isEmpty()) Text("0", style = style.copy(color = InvestColors.muted))
                    inner()
                }
            }
        },
    )
}

@Composable
private fun Presets(
    amount: TopUpAmount,
    onPreset: (Long) -> Unit,
    modifier: Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TopUpAmount.PRESETS.forEach { preset ->
            val on = amount.minor == preset
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) InvestColors.accentSoft else Color.Transparent)
                    .border(1.dp, if (on) InvestColors.accent else InvestColors.fieldBorder, RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button) { onPreset(preset) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    eurWhole(preset),
                    style = plex(14.sp, FontWeight.SemiBold, if (on) InvestColors.accent else InvestColors.text, tabular = true),
                )
            }
        }
    }
}
