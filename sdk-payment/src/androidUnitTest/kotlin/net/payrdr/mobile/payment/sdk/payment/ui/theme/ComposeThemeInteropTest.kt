package net.payrdr.mobile.payment.sdk.payment.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ComposeThemeInteropTest {
    @Test fun `a Compose colour converts both ways`() {
        Color(0x80112233).toPaymentColor() shouldBe PaymentColor(0x80112233)
        PaymentColor(0x80112233).toColor() shouldBe Color(0x80112233)
    }

    @Test fun `the Compose palette overload keeps the defaults of the derived colours`() {
        val colors =
            PaymentColors(
                primary = Color(0xFF1F4FD8),
                onPrimary = Color.White,
                background = Color.White,
                onBackground = Color(0xFF0D1726),
                surface = Color(0xFFF3F5F9),
                onSurface = Color(0xFF0D1726),
                error = Color(0xFFC42F27),
                onError = Color.White,
                outline = Color(0xFFD9DFE8),
                success = Color(0xFF0B7F57),
                scrim = Color(0x80000000),
            )

        colors.onSurfaceVariant shouldBe PaymentColor(0xFF0D1726).withAlpha(0.7f)
        colors.placeholder shouldBe PaymentColor(0xFF0D1726).withAlpha(0.5f)
        colors.outlineStrong shouldBe PaymentColor(0xFFD9DFE8)
        colors.scrim shouldBe PaymentColor(0x80000000)
    }

    @Test fun `a Compose text style keeps its font family and metrics`() {
        val family = FontFamily.Serif
        val style =
            TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = family,
                lineHeight = 24.sp,
                letterSpacing = 0.2.sp,
                fontFeatureSettings = "tnum",
            ).toPaymentTextStyle()

        style shouldBe
            PaymentTextStyle(
                size = 18f,
                weight = PaymentTextStyle.BOLD,
                lineHeight = 24f,
                letterSpacing = 0.2f,
                tabularFigures = true,
                fontFamily = PaymentFontFamily(family),
            )
        style.toTextStyle().fontFamily shouldBe family
    }

    @Test fun `units other than sp fall back to the defaults`() {
        TextStyle(fontSize = 1.2.em, lineHeight = 1.5.em).toPaymentTextStyle() shouldBe PaymentTextStyle(size = 14f)
    }

    @Test fun `the Compose typography overload keeps the SDK's styles it is not given`() {
        val typography =
            PaymentTypography(
                title = TextStyle(fontSize = 20.sp),
                body = TextStyle(fontSize = 15.sp),
                label = TextStyle(fontSize = 13.sp),
                button = TextStyle(fontSize = 16.sp),
                input = TextStyle(fontSize = 16.sp),
            )

        typography.title shouldBe PaymentTextStyle(size = 20f)
        typography.amount shouldBe PaymentTypography.Default.amount
    }
}
