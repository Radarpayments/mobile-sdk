package net.payrdr.mobile.payment.sdk.qa.theme

import androidx.compose.ui.text.font.FontFamily
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColor
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentCorners
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentFontFamily
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTextStyle
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTypography
import net.payrdr.mobile.payment.sdk.qa.config.ThemePreset
import kotlin.test.Test

class ThemePresetsTest {
    private fun ThemePreset.custom(): PaymentTheme.Custom = paymentTheme().shouldBeInstanceOf<PaymentTheme.Custom>()

    @Test
    fun `system light and dark are the sdk's themes`() {
        ThemePreset.SYSTEM.paymentTheme() shouldBe PaymentTheme.System
        ThemePreset.LIGHT.paymentTheme() shouldBe PaymentTheme.Light
        ThemePreset.DARK.paymentTheme() shouldBe PaymentTheme.Dark
    }

    @Test
    fun `rounded makes every corner a pill`() {
        val theme = ThemePreset.ROUNDED.custom()
        theme.shapes shouldBe PaymentShapes(PaymentCorners.Pill, PaymentCorners.Pill, PaymentCorners.Pill, PaymentCorners.Pill)
        theme.typography shouldBe PaymentTypography.Default
        theme.colors.primary shouldBe PaymentColor(0xFF1F5FFF)
    }

    @Test
    fun `square makes every corner zero`() {
        val zero = PaymentCorners.Radius(0f)
        val theme = ThemePreset.SQUARE.custom()
        theme.shapes shouldBe PaymentShapes(zero, zero, zero, zero)
        theme.typography shouldBe PaymentTypography.Default
        theme.colors shouldBe ThemePreset.ROUNDED.custom().colors
    }

    @Test
    fun `large text scales all eight styles by one and a half and uses a serif family`() {
        val theme = ThemePreset.LARGE_TEXT.custom()
        val serif = PaymentFontFamily(FontFamily.Serif)

        fun PaymentTextStyle.large() = copy(size = size * 1.5f, lineHeight = lineHeight * 1.5f, fontFamily = serif)
        val default = PaymentTypography.Default
        theme.typography.title shouldBe default.title.large()
        theme.typography.body shouldBe default.body.large()
        theme.typography.label shouldBe default.label.large()
        theme.typography.button shouldBe default.button.large()
        theme.typography.input shouldBe default.input.large()
        theme.typography.headline shouldBe default.headline.large()
        theme.typography.amount shouldBe default.amount.large()
        theme.typography.caption shouldBe default.caption.large()
        theme.typography.title.size shouldBe 25.5f
        theme.typography.amount.lineHeight shouldBe 51f
        theme.shapes shouldBe PaymentShapes.Default
    }

    @Test
    fun `contrast is black on white`() {
        val colors = ThemePreset.CONTRAST.custom().colors
        colors.background.argb shouldBe 0xFFFFFFFF
        colors.onBackground.argb shouldBe 0xFF000000
        colors.scrim.argb shouldBe 0xCC000000
    }

    @Test
    fun `the debug palette has fourteen different colours and the legend lists them`() {
        DEBUG_LEGEND.map { it.second.argb }.toSet().size shouldBe 14
        DEBUG_LEGEND.first() shouldBe ("primary" to PaymentColor(0xFFFF00FF))
        val colors = ThemePreset.DEBUG.custom().colors
        DEBUG_LEGEND.map { it.second } shouldBe
            listOf(
                colors.primary,
                colors.onPrimary,
                colors.background,
                colors.onBackground,
                colors.surface,
                colors.onSurface,
                colors.error,
                colors.onError,
                colors.outline,
                colors.success,
                colors.onSurfaceVariant,
                colors.placeholder,
                colors.outlineStrong,
                colors.scrim,
            )
    }
}
