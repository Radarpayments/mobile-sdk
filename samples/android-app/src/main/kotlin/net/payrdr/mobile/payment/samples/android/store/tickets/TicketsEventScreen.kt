package net.payrdr.mobile.payment.samples.android.store.tickets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronLeft
import compose.icons.tablericons.Minus
import compose.icons.tablericons.Plus
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.model.Catalog
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Product
import net.payrdr.mobile.payment.samples.android.store.model.TICKET_FEE_ID
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import net.payrdr.mobile.payment.samples.android.store.theme.SystemBarIcons
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle

/** The concert: a full-bleed poster, seat categories, how many seats and "Checkout". */
@Composable
fun TicketsEventScreen(
    order: TicketOrder,
    holdExpired: Boolean,
    onBack: () -> Unit,
    onCategory: (String) -> Unit,
    onQuantity: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val style = coverStyle(CoverId.TICKETS)
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()
    val seats = order.toCart().lines.first()
    // The poster runs under the status bar: light icons on the dark photo.
    SystemBarIcons(lightBackground = false)
    Column(Modifier.fillMaxSize().background(style.background)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Poster(onBack)
            if (holdExpired) {
                Text(
                    stringResource(R.string.tickets_hold_expired),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = Color(0xFFC2410C),
                )
            }
            Text(
                stringResource(R.string.tickets_section),
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp),
                color = style.text,
            )
            Catalog.products(CoverId.TICKETS).filter { it.id != TICKET_FEE_ID }.forEach { category ->
                CategoryRow(category, selected = category.id == order.categoryId, locale = locale) { onCategory(category.id) }
            }
        }
        Row(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(style.radius))
                .background(style.primary)
                .padding(start = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onQuantity(order.quantity - 1) }) {
                Icon(TablerIcons.Minus, contentDescription = stringResource(R.string.store_decrease), tint = style.onPrimary)
            }
            Text(
                order.quantity.toString(),
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                color = style.onPrimary,
            )
            IconButton(onClick = { onQuantity(order.quantity + 1) }) {
                Icon(TablerIcons.Plus, contentDescription = stringResource(R.string.store_increase), tint = style.onPrimary)
            }
            Text(
                stringResource(R.string.tickets_to_payment, formatEur(seats.product.priceMinor * seats.quantity, locale)),
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable(role = Role.Button, onClick = onNext)
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp),
                color = style.onPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: Product,
    selected: Boolean,
    locale: java.util.Locale,
    onClick: () -> Unit,
) {
    val style = coverStyle(CoverId.TICKETS)
    val on = selected
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(style.radius))
            .background(if (on) style.surface else style.background)
            .border(if (on) 2.dp else 1.dp, if (on) style.primary else style.border, RoundedCornerShape(style.radius))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(category.name),
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                color = style.text,
            )
            category.subtitle?.let {
                Text(stringResource(it), style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp), color = style.muted)
            }
        }
        Text(
            formatEur(category.priceMinor, locale),
            style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
            color = style.text,
        )
    }
}

@Composable
private fun Poster(onBack: () -> Unit) {
    val style = coverStyle(CoverId.TICKETS)
    Box(Modifier.fillMaxWidth().height(340.dp)) {
        Image(
            painterResource(R.drawable.tickets_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.25f), 1f to Color.Black.copy(alpha = 0.8f))),
        )
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.35f)),
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape),
        ) {
            Icon(TablerIcons.ChevronLeft, contentDescription = stringResource(R.string.store_back), tint = Color.White)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                stringResource(R.string.tickets_live),
                style =
                    TextStyle(
                        fontFamily = style.accentFont,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    ),
                color = Color.White,
            )
            Text(
                stringResource(R.string.tickets_title),
                style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 40.sp),
                color = Color.White,
            )
            Text(
                stringResource(R.string.tickets_date),
                modifier = Modifier.padding(top = 6.dp),
                style = TextStyle(fontFamily = style.bodyFont, fontSize = 14.sp),
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}
