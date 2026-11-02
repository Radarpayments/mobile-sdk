package net.payrdr.mobile.payment.samples.android.ui.components

import androidx.compose.ui.graphics.Color

/** Flat brand-color palette used for buttons, icons, and gradients. */
object BrandColors {
    val Indigo = Color(0xFF6366F1)
    val Violet = Color(0xFF8B5CF6)
    val Emerald = Color(0xFF10B981)
    val EmeraldLight = Color(0xFF34D399)
    val Amber = Color(0xFFF59E0B)
    val AmberLight = Color(0xFFFBBF24)
    val Rose = Color(0xFFE11D48)
    val RoseLight = Color(0xFFFB7185)
    val Sky = Color(0xFF0EA5E9)
    val SkyLight = Color(0xFF38BDF8)
    val VioletDark = Color(0xFF9333EA)
    val VioletLight = Color(0xFFC084FC)
    val HotPink = Color(0xFFF093FB)
    val HotPinkDeep = Color(0xFFF5576C)
    val IndigoLight = Color(0xFF818CF8)
    val SuccessGreen = Color(0xFF22C55E)
}

/** Named 2-stop gradients used throughout the sample for hero/tile backgrounds. */
data class Gradient(
    val colorStops: List<Color>,
)

object BrandGradients {
    val Indigo = Gradient(listOf(BrandColors.IndigoLight, BrandColors.Indigo))
    val Emerald = Gradient(listOf(BrandColors.EmeraldLight, BrandColors.Emerald))
    val Amber = Gradient(listOf(BrandColors.AmberLight, BrandColors.Amber))
    val Rose = Gradient(listOf(BrandColors.RoseLight, BrandColors.Rose))
    val Sky = Gradient(listOf(BrandColors.SkyLight, BrandColors.Sky))
    val Violet = Gradient(listOf(BrandColors.VioletLight, BrandColors.VioletDark))
    val HotPink = Gradient(listOf(BrandColors.HotPink, BrandColors.HotPinkDeep))
    val Success = Gradient(listOf(BrandColors.SuccessGreen, BrandColors.Emerald))
}
