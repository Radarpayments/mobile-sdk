package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PaymentThemeTypesTest {
    private val ink = PaymentColor(0xFF0D1726)
    private val line = PaymentColor(0xFFD9DFE8)

    private fun colors() =
        PaymentColors(
            primary = PaymentColor(0xFF1F4FD8),
            onPrimary = PaymentColor.White,
            background = PaymentColor.White,
            onBackground = ink,
            surface = PaymentColor(0xFFF3F5F9),
            onSurface = ink,
            error = PaymentColor(0xFFC42F27),
            onError = PaymentColor.White,
            outline = line,
            success = PaymentColor(0xFF0B7F57),
        )

    @Test fun `a colour splits into its channels`() {
        val color = PaymentColor(0x80112233)

        listOf(color.alpha, color.red, color.green, color.blue) shouldBe listOf(0x80, 0x11, 0x22, 0x33)
        color.toString() shouldBe "PaymentColor(#80112233)"
    }

    @Test fun `rgb builds the same colour as the hex literal`() {
        PaymentColor.rgb(0x1F, 0x4F, 0xD8) shouldBe PaymentColor(0xFF1F4FD8)
        PaymentColor.rgb(0, 0, 0, alpha = 0.45f) shouldBe PaymentColor.Black.withAlpha(0.45f)
    }

    @Test fun `withAlpha rounds the way Compose copy does`() {
        for (alpha in listOf(0f, 0.45f, 0.5f, 0.7f, 1f)) {
            PaymentColor(0xFF0D1726).withAlpha(alpha).toComposeColor() shouldBe Color(0xFF0D1726).copy(alpha = alpha)
        }
    }

    @Test fun `out of range values are refused`() {
        assertFailsWith<IllegalArgumentException> { PaymentColor(0x1_0000_0000) }
        assertFailsWith<IllegalArgumentException> { PaymentColor(-1) }
        assertFailsWith<IllegalArgumentException> { PaymentColor.Black.withAlpha(1.5f) }
        assertFailsWith<IllegalArgumentException> { PaymentColor.rgb(256, 0, 0) }
        assertFailsWith<IllegalArgumentException> { PaymentTextStyle(size = 0f) }
        assertFailsWith<IllegalArgumentException> { PaymentCorners.Radius(-1f) }
    }

    @Test fun `the derived colours default as before`() {
        val colors = colors()

        colors.onSurfaceVariant shouldBe ink.withAlpha(0.7f)
        colors.placeholder shouldBe ink.withAlpha(0.5f)
        colors.outlineStrong shouldBe line
        colors.scrim shouldBe PaymentColor(0x73000000)
    }

    @Test fun `the Compose colours are the palette's`() {
        val colors = ThemeColors(DefaultLightColors)

        colors.primary shouldBe Color(0xFF1F4FD8)
        colors.scrim shouldBe Color(0x6B0D1726)
    }

    @Test fun `a palette is dark by its background`() {
        DefaultDarkColors.isDark shouldBe true
        DefaultLightColors.isDark shouldBe false
        colors().copy(background = PaymentColor(0xFF808080)).isDark shouldBe false
        colors().copy(background = PaymentColor(0xFF7F7F7F)).isDark shouldBe true
    }

    @Test fun `a text style becomes the Compose one the theme had`() {
        PaymentTypography.Default.amount.toTextStyle().let {
            it.fontSize shouldBe 30.sp
            it.fontWeight shouldBe FontWeight.SemiBold
            it.lineHeight shouldBe 34.sp
            it.letterSpacing shouldBe (-0.5).sp
            it.fontFeatureSettings shouldBe TABULAR
        }
        PaymentTypography.Default.button.toTextStyle().let {
            it.fontSize shouldBe 16.sp
            it.lineHeight shouldBe TextUnit.Unspecified
            it.letterSpacing shouldBe TextUnit.Unspecified
            it.fontFeatureSettings shouldBe null
            it.fontFamily shouldBe null
        }
        PaymentTypography.Default.caption
            .toTextStyle()
            .fontWeight shouldBe FontWeight.Normal
    }

    @Test fun `corners become rounded shapes`() {
        PaymentCorners.Radius(12f).toShape() shouldBe RoundedCornerShape(12.dp)
        PaymentCorners.Pill.toShape() shouldBe RoundedCornerShape(percent = 50)
        ThemeShapes.Default.sheet shouldBe RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    }

    @Test fun `a pill sheet keeps the default top corners`() {
        PaymentShapes.Default.copy(sheet = PaymentCorners.Pill).sheetRadius shouldBe 26f
        PaymentShapes.Default.copy(sheet = PaymentCorners.Radius(8f)).sheetRadius shouldBe 8f
    }
}
