package net.payrdr.mobile.payment.sdk.qa.theme

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColor
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentCorners
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
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
        val typography = theme.typography
        typography.title.size shouldBe 25.5f
        typography.title.lineHeight shouldBe 33f
        typography.amount.size shouldBe 45f
        typography.caption.size shouldBe 18.75f
        val styles =
            listOf(
                typography.title,
                typography.body,
                typography.label,
                typography.button,
                typography.input,
                typography.headline,
                typography.amount,
                typography.caption,
            )
        styles.forEach { it.fontFamily.shouldNotBeNull() }
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
