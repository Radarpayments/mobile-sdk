package net.payrdr.mobile.payment.samples.android.store.furniture

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import compose.icons.TablerIcons
import compose.icons.tablericons.Basket
import compose.icons.tablericons.ChevronLeft
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.model.Cart
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Product
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle

/** Catalogue: brand header with the cart, collection photo, category chips and a product grid. */
@Composable
fun FurnitureCatalogScreen(
    cart: Cart,
    onBack: () -> Unit,
    onProduct: (Product) -> Unit,
    onCart: () -> Unit,
    grid: LazyGridState = rememberLazyGridState(),
    category: FurnitureCategory = FurnitureCategory.ALL,
    onCategory: (FurnitureCategory) -> Unit = {},
    flight: PhotoFlight = { this },
) {
    val style = coverStyle(CoverId.FURNITURE)
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()
    Box(Modifier.fillMaxSize().background(style.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (cart.isEmpty) 16.dp else 88.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(2) }) { Header(cart.count, onBack, onCart) }
            item(span = { GridItemSpan(2) }) { Hero() }
            item(span = { GridItemSpan(2) }) { Chips(category, onCategory) }
            items(furnitureFilter(category), key = { it.id }) { product ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(style.radius))
                        .clickable(role = Role.Button) { onProduct(product) },
                ) {
                    product.image?.let {
                        Image(
                            painterResource(it),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .flight(product)
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(style.radius)),
                        )
                    }
                    Text(
                        stringResource(product.name),
                        modifier = Modifier.padding(top = 8.dp),
                        style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                        color = style.text,
                    )
                    Text(
                        formatEur(product.priceMinor, locale),
                        style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp),
                        color = style.muted,
                    )
                }
            }
        }
        if (!cart.isEmpty) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(style.radius))
                    .background(style.primary)
                    .clickable(role = Role.Button, onClick = onCart)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.store_cart_count, cart.count),
                    modifier = Modifier.weight(1f),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = style.onPrimary,
                )
                Text(
                    formatEur(cart.totalMinor, locale),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = style.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun Header(
    count: Int,
    onBack: () -> Unit,
    onCart: () -> Unit,
) {
    val style = coverStyle(CoverId.FURNITURE)
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(TablerIcons.ChevronLeft, contentDescription = stringResource(R.string.store_back), tint = style.text)
        }
        Text(
            stringResource(R.string.cover_furniture_name),
            modifier = Modifier.weight(1f).offset(x = (-8).dp),
            style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.Bold, fontSize = 26.sp),
            color = style.text,
        )
        val cartLabel = stringResource(R.string.store_cart_count, count)
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onCart)
                .semantics { contentDescription = cartLabel },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(40.dp).border(1.dp, style.border, CircleShape), contentAlignment = Alignment.Center) {
                Icon(TablerIcons.Basket, contentDescription = null, tint = style.text, modifier = Modifier.size(20.dp))
            }
            if (count > 0) {
                Text(
                    count.toString(),
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(style.accent)
                            .padding(horizontal = 6.dp),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun Hero() {
    val style = coverStyle(CoverId.FURNITURE)
    Box(Modifier.fillMaxWidth().height(184.dp).clip(RoundedCornerShape(16.dp))) {
        Image(
            painterResource(R.drawable.furniture_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f))))
        Text(
            stringResource(R.string.furniture_collection),
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 26.sp),
            color = Color.White,
        )
    }
}

@Composable
private fun Chips(
    selected: FurnitureCategory,
    onSelect: (FurnitureCategory) -> Unit,
) {
    val style = coverStyle(CoverId.FURNITURE)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FurnitureCategory.entries.forEach { category ->
            val on = category == selected
            // The text sits in a Box: a Text stretched to the minimum height keeps its line at the top.
            Box(
                modifier =
                    Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (on) style.primary else Color.Transparent)
                        .border(1.dp, if (on) style.primary else style.border, RoundedCornerShape(20.dp))
                        .clickable(interactionSource = null, indication = null, role = Role.Tab) { onSelect(category) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(category.label),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                    color = if (on) style.onPrimary else style.text,
                )
            }
        }
    }
}
