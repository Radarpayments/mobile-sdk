package net.payrdr.mobile.payment.samples.android.store.furniture

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Product
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle

/** One product: a large photo, the name, colours, delivery terms and "Add to cart". */
@Composable
fun FurnitureProductScreen(
    product: Product,
    onBack: () -> Unit,
    onAdd: (Product) -> Unit,
    flight: PhotoFlight = { this },
) {
    val style = coverStyle(CoverId.FURNITURE)
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()
    Column(Modifier.fillMaxSize().background(style.background)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(PHOTO_SHARE)) {
            product.image?.let {
                Image(
                    painterResource(it),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.flight(product).fillMaxSize(),
                )
            }
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.92f)),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(12.dp),
            ) {
                Icon(TablerIcons.ChevronLeft, contentDescription = stringResource(R.string.store_back), tint = style.text)
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                stringResource(product.name),
                style = TextStyle(fontFamily = style.headingFont, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 34.sp),
                color = style.text,
            )
            product.subtitle?.let {
                Text(stringResource(it), style = TextStyle(fontFamily = style.bodyFont, fontSize = 14.sp), color = style.muted)
            }
            Text(
                stringResource(R.string.furniture_colours),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                color = style.text,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Color(0xFF1C1917), Color(0xFFE7E5E4), style.accent).forEachIndexed { i, colour ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .border(if (i == 0) 2.dp else 1.dp, if (i == 0) style.text else style.border, CircleShape)
                            .padding(4.dp)
                            .background(colour, CircleShape),
                    )
                }
            }
            Text(
                stringResource(R.string.furniture_delivery),
                modifier = Modifier.padding(top = 16.dp),
                style = TextStyle(fontFamily = style.bodyFont, fontSize = 14.sp, lineHeight = 20.sp),
                color = style.muted,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onAdd(product) },
                shape = RoundedCornerShape(style.radius),
                colors = ButtonDefaults.buttonColors(containerColor = style.primary, contentColor = style.onPrimary),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Text(
                    stringResource(R.string.store_add_to_cart, formatEur(product.priceMinor, locale)),
                    style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                )
            }
        }
    }
}

private const val PHOTO_SHARE = 0.46f
