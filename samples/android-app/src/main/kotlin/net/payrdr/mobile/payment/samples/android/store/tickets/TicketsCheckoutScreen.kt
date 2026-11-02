package net.payrdr.mobile.payment.samples.android.store.tickets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronLeft
import compose.icons.tablericons.Clock
import kotlinx.coroutines.delay
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.store.StoreCheckoutBar
import net.payrdr.mobile.payment.samples.android.store.model.CartLine
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import net.payrdr.mobile.payment.samples.android.store.theme.SystemBarIcons
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle

/** Checkout: the event, the bill, a seat hold that runs out, and the shared payment bar. */
@Composable
fun TicketsCheckoutScreen(
    order: TicketOrder,
    onBack: () -> Unit,
    onHoldExpired: () -> Unit,
    onOutcome: (CheckoutOutcome, Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val style = coverStyle(CoverId.TICKETS)
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()
    val cart = order.toCart()
    val (seats, fee) = cart.lines
    SystemBarIcons(lightBackground = true)
    var secondsLeft by rememberSaveable { mutableIntStateOf(HOLD_SECONDS) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(SECOND_MS)
            secondsLeft--
        }
        onHoldExpired()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(style.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            IconButton(onClick = onBack) {
                Icon(TablerIcons.ChevronLeft, contentDescription = stringResource(R.string.store_back), tint = style.text)
            }
            Text(
                stringResource(R.string.tickets_checkout),
                style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                color = style.text,
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            EventCard(seats, locale)
            Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Line(stringResource(R.string.tickets_seats), formatEur(seats.product.priceMinor * seats.quantity, locale), bold = false)
                Line(stringResource(fee.product.name), formatEur(fee.product.priceMinor, locale), bold = false)
                Line(stringResource(R.string.store_total), formatEur(cart.totalMinor, locale), bold = true)
            }
            HoldLine(secondsLeft)
        }
        StoreCheckoutBar(
            cover = CoverId.TICKETS,
            amountMinor = cart.totalMinor,
            onOutcome = { onOutcome(it, cart.totalMinor) },
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
    bold: Boolean,
) {
    val style = coverStyle(CoverId.TICKETS)
    val text =
        TextStyle(
            fontFamily = style.bodyFont,
            fontSize = if (bold) 17.sp else 14.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal,
        )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = text, color = if (bold) style.text else style.muted)
        Text(value, style = text, color = if (bold) style.text else style.muted)
    }
}

private const val HOLD_SECONDS = 600
private const val SECOND_MS = 1000L
private const val SECONDS_IN_MINUTE = 60

// Orange-700: the hold warning, 4.5:1 on white.
private val HOLD_COLOUR = Color(0xFFC2410C)

@Composable
private fun EventCard(
    seats: CartLine,
    locale: java.util.Locale,
) {
    val style = coverStyle(CoverId.TICKETS)
    Row(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(style.radius))
            .border(1.dp, style.border, RoundedCornerShape(style.radius)),
    ) {
        Image(
            painterResource(R.drawable.tickets_crowd),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(96.dp).height(96.dp),
        )
        Column(Modifier.padding(12.dp)) {
            Text(
                stringResource(R.string.tickets_title).replace('\n', ' '),
                style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                color = style.text,
            )
            Text(
                stringResource(R.string.tickets_event_line, stringResource(seats.product.name)),
                style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp),
                color = style.muted,
            )
            Text(
                "${seats.quantity} × ${formatEur(seats.product.priceMinor, locale)}",
                style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp),
                color = style.muted,
            )
        }
    }
}

@Composable
private fun HoldLine(secondsLeft: Int) {
    val style = coverStyle(CoverId.TICKETS)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(TablerIcons.Clock, contentDescription = null, tint = HOLD_COLOUR, modifier = Modifier.size(18.dp))
        Text(
            stringResource(
                R.string.tickets_hold,
                "%d:%02d".format(secondsLeft / SECONDS_IN_MINUTE, secondsLeft % SECONDS_IN_MINUTE),
            ),
            modifier = Modifier.padding(start = 6.dp),
            style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
            color = HOLD_COLOUR,
        )
    }
}
