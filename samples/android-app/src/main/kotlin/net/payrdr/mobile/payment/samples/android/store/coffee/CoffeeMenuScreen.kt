package net.payrdr.mobile.payment.samples.android.store.coffee

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronLeft
import compose.icons.tablericons.MapPin
import compose.icons.tablericons.Plus
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.model.Cart
import net.payrdr.mobile.payment.samples.android.store.model.Catalog
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Product
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle

private enum class CoffeeTab(
    val label: Int,
) {
    COFFEE(R.string.coffee_tab_coffee),
    COLD(R.string.coffee_tab_cold),
    BAKERY(R.string.coffee_tab_bakery),
}

private fun tabOf(id: String) =
    when (id) {
        "iced_latte" -> CoffeeTab.COLD
        "croissant" -> CoffeeTab.BAKERY
        else -> CoffeeTab.COFFEE
    }

/** Menu: the last order to repeat in one tap, tabs, items with "+", and the order pill. */
@Composable
fun CoffeeMenuScreen(
    cart: Cart,
    lastPaid: Cart?,
    onBack: () -> Unit,
    onAdd: (Product) -> Unit,
    onRepeat: () -> Unit,
    onOrder: () -> Unit,
) {
    val style = coverStyle(CoverId.COFFEE)
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()
    var tab by rememberSaveable { mutableStateOf(CoffeeTab.COFFEE) }
    Box(Modifier.fillMaxSize().background(style.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(TablerIcons.ChevronLeft, contentDescription = stringResource(R.string.store_back), tint = style.text)
                }
                Text(
                    stringResource(R.string.cover_coffee_name),
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
                    color = style.text,
                )
                Icon(TablerIcons.MapPin, contentDescription = null, tint = style.muted, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.coffee_location),
                    modifier = Modifier.padding(start = 4.dp),
                    style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp),
                    color = style.muted,
                )
            }
            lastPaid?.takeIf { !it.isEmpty }?.let { Again(it, onRepeat) }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                CoffeeTab.entries.forEach { t ->
                    val on = t == tab
                    Column(
                        Modifier.clickable(interactionSource = null, indication = null, role = Role.Tab) { tab = t }.heightIn(min = 44.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            stringResource(t.label),
                            style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = if (on) style.text else style.muted,
                        )
                        Box(
                            Modifier
                                .padding(top = 4.dp)
                                .height(2.dp)
                                .width(28.dp)
                                .background(if (on) style.primary else style.background),
                        )
                    }
                }
            }
            Catalog.products(CoverId.COFFEE).filter { tabOf(it.id) == tab }.forEach { product ->
                MenuRow(product, locale) { onAdd(product) }
            }
        }
        if (!cart.isEmpty) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(style.primary)
                    .clickable(role = Role.Button, onClick = onOrder)
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.store_cart_count, cart.count),
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                    color = style.onPrimary,
                )
                Text(
                    formatEur(cart.totalMinor, locale),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp),
                    color = style.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun Again(
    last: Cart,
    onRepeat: () -> Unit,
) {
    val style = coverStyle(CoverId.COFFEE)
    val context = LocalContext.current
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(style.radius))
            .background(style.surface),
    ) {
        Image(
            painterResource(R.drawable.coffee_flat_white),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(104.dp).fillMaxHeight(),
        )
        Column(Modifier.weight(1f).padding(12.dp)) {
            Text(
                stringResource(R.string.coffee_again),
                style = TextStyle(fontFamily = style.bodyFont, fontSize = 12.sp),
                color = style.muted,
            )
            Text(
                last.lines.joinToString { context.getString(it.product.name) },
                maxLines = 1,
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = style.text,
            )
            Text(formatEur(last.totalMinor, locale), style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp), color = style.muted)
            Text(
                stringResource(R.string.coffee_repeat),
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(style.primary)
                        .clickable(role = Role.Button, onClick = onRepeat)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp),
                color = style.onPrimary,
            )
        }
    }
}

@Composable
private fun MenuRow(
    product: Product,
    locale: java.util.Locale,
    onAdd: () -> Unit,
) {
    val style = coverStyle(CoverId.COFFEE)
    val name = stringResource(product.name)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        product.image?.let {
            Image(
                painterResource(it),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(name, style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = style.text)
            product.subtitle?.let {
                Text(
                    stringResource(it),
                    style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp),
                    color = style.muted,
                    maxLines = 1,
                )
            }
            Text(
                formatEur(product.priceMinor, locale),
                style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp),
                color = style.muted,
            )
        }
        IconButton(onClick = onAdd) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(style.primary), contentAlignment = Alignment.Center) {
                Icon(
                    TablerIcons.Plus,
                    contentDescription = stringResource(R.string.coffee_add_item, name),
                    tint = style.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
