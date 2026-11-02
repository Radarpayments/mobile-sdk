package net.payrdr.mobile.payment.samples.android.store

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.Settings
import compose.icons.tablericons.TrendingUp
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.store.invest.ChartPeriod
import net.payrdr.mobile.payment.samples.android.store.invest.InvestColors
import net.payrdr.mobile.payment.samples.android.store.invest.PortfolioChart
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.theme.StoreFonts
import net.payrdr.mobile.payment.samples.android.store.theme.SystemBarIcons
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle

/** First screen: the SDK Payment stores, SDK Core's own card form, and the developer settings. */
@Composable
fun StartScreen(
    aftDemo: Boolean,
    onCover: (CoverId) -> Unit,
    onSettings: () -> Unit,
    onCore: () -> Unit,
) {
    SystemBarIcons(lightBackground = true)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(StartColors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // At least a screen tall: the store tiles share what the rest leaves, so SDK Core sits at the bottom.
        // On a short screen the tiles keep their minimum height and the page scrolls instead.
        StartContent(minHeight = maxHeight, aftDemo = aftDemo, onCover = onCover, onSettings = onSettings, onCore = onCore)
    }
}

@Composable
private fun StartContent(
    minHeight: Dp,
    aftDemo: Boolean,
    onCover: (CoverId) -> Unit,
    onSettings: () -> Unit,
    onCore: () -> Unit,
) {
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .heightIn(min = minHeight)
            .padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.start_title),
                modifier = Modifier.weight(1f),
                style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp),
                color = StartColors.text,
            )
            IconButton(onClick = onSettings) {
                Icon(TablerIcons.Settings, contentDescription = stringResource(R.string.start_settings), tint = StartColors.text)
            }
        }
        // AFT orders top up an account: shown in a business where that is what happens, not in the stores.
        Section(R.string.start_section_payment, if (aftDemo) R.string.start_section_payment_aft_sub else R.string.start_section_payment_sub)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (aftDemo) {
                InvestTile(onClick = { onCover(CoverId.INVEST) }, modifier = Modifier.weight(1f).heightIn(min = COVER_CARD_HEIGHT * 2))
                return@Column
            }
            val tile = Modifier.weight(1f).heightIn(min = COVER_CARD_HEIGHT)
            CoverCard(
                CoverId.FURNITURE,
                R.drawable.furniture_hero,
                R.string.cover_furniture_name,
                R.string.cover_furniture_tagline,
                onCover,
                tile,
            )
            CoverCard(CoverId.COFFEE, R.drawable.coffee_hero, R.string.cover_coffee_name, R.string.cover_coffee_tagline, onCover, tile)
            CoverCard(CoverId.TICKETS, R.drawable.tickets_hero, R.string.cover_tickets_name, R.string.cover_tickets_tagline, onCover, tile)
        }
        Section(R.string.start_section_core, R.string.start_section_core_sub)
        CoreCard(onCore)
        Spacer(Modifier.height(16.dp))
    }
}

/** Heading of one SDK on the start screen. */
@Composable
private fun Section(
    @StringRes title: Int,
    @StringRes subtitle: Int,
) {
    Column(Modifier.padding(top = 20.dp, bottom = 10.dp)) {
        Text(
            stringResource(title),
            style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
            color = StartColors.text,
        )
        Text(
            stringResource(subtitle),
            modifier = Modifier.padding(top = 2.dp),
            style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 13.sp, lineHeight = 18.sp),
            color = StartColors.muted,
        )
    }
}

/** SDK Core: a photo tile like the stores'. */
@Composable
private fun CoreCard(onCore: () -> Unit) {
    PhotoTile(
        image = R.drawable.core_hero,
        name = stringResource(R.string.core_tile_title),
        nameStyle = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
        tagline = stringResource(R.string.core_tile_sub),
        onClick = onCore,
    )
}

private val COVER_CARD_HEIGHT = 112.dp

@Composable
private fun CoverCard(
    cover: CoverId,
    @DrawableRes image: Int,
    @StringRes name: Int,
    @StringRes tagline: Int,
    onCover: (CoverId) -> Unit,
    modifier: Modifier,
) {
    PhotoTile(
        image = image,
        name = stringResource(name),
        nameStyle = TextStyle(fontFamily = coverStyle(cover).headingFont, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        tagline = stringResource(tagline),
        onClick = { onCover(cover) },
        modifier = modifier,
    )
}

/** Photo with a dark fade at the bottom, the name and a tagline over it. */
@Composable
private fun PhotoTile(
    @DrawableRes image: Int,
    name: String,
    nameStyle: TextStyle,
    tagline: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.height(COVER_CARD_HEIGHT),
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Image(painterResource(image), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.68f))),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(name, style = nameStyle, color = Color.White)
            Text(
                tagline,
                style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.92f),
            )
        }
    }
}

/** Northwind Invest's tile: no photo, its own colours and a rising line. */
@Composable
private fun InvestTile(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF166534), InvestColors.surface), radius = 900f, center = Offset(900f, 0f)))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(TablerIcons.TrendingUp, contentDescription = null, tint = InvestColors.accent, modifier = Modifier.size(22.dp))
            Text(
                "Northwind",
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                style = TextStyle(fontFamily = StoreFonts.plex, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = InvestColors.text,
            )
            Text(
                "AFT",
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(InvestColors.accent.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                style = TextStyle(fontFamily = StoreFonts.plex, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                color = InvestColors.accent,
            )
        }
        PortfolioChart(ChartPeriod.ALL, Modifier.weight(1f).padding(vertical = 12.dp))
        Text(
            stringResource(R.string.invest_name),
            style = TextStyle(fontFamily = StoreFonts.plex, fontWeight = FontWeight.Bold, fontSize = 24.sp),
            color = InvestColors.text,
        )
        Text(
            stringResource(R.string.invest_tile_sub),
            style = TextStyle(fontFamily = StoreFonts.plex, fontSize = 13.sp, lineHeight = 18.sp),
            color = Color(0xFFCBD5E1),
        )
    }
}

/** Neutral colours of the start and developer screens: they belong to the demo, not to a store. */
internal object StartColors {
    val background = Color(0xFFF7F7F5)
    val surface = Color(0xFFFFFFFF)
    val text = Color(0xFF111827)
    val muted = Color(0xFF4B5563)
    val border = Color(0xFFE5E7EB)
    val link = Color(0xFF2563EB)
    val selected = Color(0xFFE5E7EB)

    /** Material colours for the neutral screens: grey selection instead of the default purple. */
    val scheme =
        lightColorScheme(
            primary = text,
            surface = surface,
            secondaryContainer = selected,
            onSecondaryContainer = text,
        )
}
