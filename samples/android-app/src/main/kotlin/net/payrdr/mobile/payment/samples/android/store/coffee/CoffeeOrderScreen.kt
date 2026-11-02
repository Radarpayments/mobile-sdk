package net.payrdr.mobile.payment.samples.android.store.coffee

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

/** The order: items with quantities, where to pick it up, and the shared payment bar. */
@Composable
fun CoffeeOrderScreen(
    cart: Cart,
    autoPay: Boolean,
    onAutoPayStarted: () -> Unit,
    onBack: () -> Unit,
    onQuantity: (productId: String, quantity: Int) -> Unit,
    onOutcome: (CheckoutOutcome, Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val style = coverStyle(CoverId.COFFEE)
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
                stringResource(R.string.coffee_order_title),
                style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
                color = style.text,
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            cart.lines.forEach { line -> OrderLine(line, locale, onQuantity) }
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(style.radius))
                    .background(style.surface)
                    .padding(14.dp),
            ) {
                Text(
                    stringResource(R.string.coffee_pickup),
                    modifier = Modifier.padding(end = 10.dp),
                    style = TextStyle(fontFamily = style.bodyFont, fontSize = 14.sp),
                    color = style.muted,
                )
                Text(
                    stringResource(R.string.coffee_pickup_value),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = style.text,
                )
            }
        }
        StoreCheckoutBar(
            cover = CoverId.COFFEE,
            amountMinor = cart.totalMinor,
            onOutcome = { onOutcome(it, cart.totalMinor) },
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(vertical = 12.dp),
            autoPay = autoPay,
            onAutoPayStarted = onAutoPayStarted,
        )
    }
}

@Composable
private fun OrderLine(
    line: CartLine,
    locale: java.util.Locale,
    onQuantity: (String, Int) -> Unit,
) {
    val style = coverStyle(CoverId.COFFEE)
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        line.product.image?.let {
            Image(
                painterResource(it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                stringResource(line.product.name),
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = style.text,
            )
            Text(
                formatEur(line.product.priceMinor * line.quantity, locale),
                style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp),
                color = style.muted,
            )
        }
        IconButton(onClick = { onQuantity(line.product.id, line.quantity - 1) }) {
            Icon(TablerIcons.Minus, contentDescription = stringResource(R.string.store_decrease), tint = style.text)
        }
        Text(
            line.quantity.toString(),
            style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold),
            color = style.text,
        )
        IconButton(onClick = { onQuantity(line.product.id, line.quantity + 1) }) {
            Icon(TablerIcons.Plus, contentDescription = stringResource(R.string.store_increase), tint = style.text)
        }
    }
}
