package net.payrdr.mobile.payment.samples.android.store.furniture

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import compose.icons.tablericons.Minus
import compose.icons.tablericons.Plus
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.store.StoreCheckoutBar
import net.payrdr.mobile.payment.samples.android.store.model.Cart
import net.payrdr.mobile.payment.samples.android.store.model.CartLine
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle

/** The cart: lines with a quantity stepper, totals, then the shared scenario row and "Pay". */
@Composable
fun FurnitureCartScreen(
    cart: Cart,
    onBack: () -> Unit,
    onQuantity: (productId: String, quantity: Int) -> Unit,
    onOutcome: (CheckoutOutcome, Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val style = coverStyle(CoverId.FURNITURE)
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()
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
                stringResource(R.string.store_cart),
                style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.Bold, fontSize = 26.sp),
                color = style.text,
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            cart.lines.forEach { line ->
                CartRow(line, onQuantity)
                HorizontalDivider(color = style.border.copy(alpha = 0.6f))
            }
            Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SumRow(stringResource(R.string.store_items), formatEur(cart.totalMinor, locale), bold = false)
                SumRow(stringResource(R.string.furniture_delivery_label), stringResource(R.string.furniture_delivery_free), bold = false)
                SumRow(stringResource(R.string.store_total), formatEur(cart.totalMinor, locale), bold = true)
            }
        }
        StoreCheckoutBar(
            cover = CoverId.FURNITURE,
            amountMinor = cart.totalMinor,
            onOutcome = { onOutcome(it, cart.totalMinor) },
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun CartRow(
    line: CartLine,
    onQuantity: (String, Int) -> Unit,
) {
    val style = coverStyle(CoverId.FURNITURE)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        line.product.image?.let {
            Image(
                painterResource(it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                stringResource(line.product.name),
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                color = style.text,
            )
            line.product.subtitle?.let {
                Text(
                    stringResource(it),
                    style = TextStyle(fontFamily = style.bodyFont, fontSize = 12.sp),
                    color = style.muted,
                    maxLines = 1,
                )
            }
        }
        Row(
            Modifier.border(1.dp, style.border, RoundedCornerShape(10.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onQuantity(line.product.id, line.quantity - 1) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    TablerIcons.Minus,
                    contentDescription = stringResource(R.string.store_decrease),
                    tint = style.text,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                line.quantity.toString(),
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold),
                color = style.text,
            )
            IconButton(onClick = { onQuantity(line.product.id, line.quantity + 1) }, modifier = Modifier.size(40.dp)) {
                Icon(
                    TablerIcons.Plus,
                    contentDescription = stringResource(R.string.store_increase),
                    tint = style.text,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SumRow(
    label: String,
    value: String,
    bold: Boolean,
) {
    val style = coverStyle(CoverId.FURNITURE)
    val text =
        TextStyle(
            fontFamily = style.bodyFont,
            fontSize = if (bold) 16.sp else 14.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = text, color = if (bold) style.text else style.muted)
        Text(value, style = text, color = if (bold) style.text else style.muted)
    }
}
