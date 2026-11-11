package net.payrdr.mobile.payment.sdk.payment.presentation

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultDarkColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentCorners
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentFontFamily
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTypography
import kotlin.test.Test

@OptIn(PayrdrUiApi::class)
class ThemeSpecTest {
    @Test
    fun the_default_theme_keeps_the_sdk_metrics() {
        val spec = ThemeSpec.of(PaymentTheme.System)

        spec.followsSystem shouldBe true
        spec.amount.size shouldBe 30.0
        spec.amount.weight shouldBe 600
        spec.amount.tracking shouldBe -0.5
        spec.amount.tabularFigures shouldBe true
        spec.button.lineHeight shouldBe 0.0
        spec.body.fontFamily shouldBe null
        listOf(spec.fieldCorner, spec.buttonCorner, spec.sheetCorner, spec.cardCorner) shouldBe listOf(12.0, 14.0, 26.0, 14.0)
    }

    @Test
    fun a_custom_theme_carries_its_font_family_name() {
        val inter = PaymentFontFamily("Inter")
        val typography = PaymentTypography.Default.let { it.copy(body = it.body.copy(fontFamily = inter)) }

        val spec = ThemeSpec.of(PaymentTheme.Custom(colors = DefaultDarkColors, typography = typography))

        spec.body.fontFamily shouldBe "Inter"
        spec.title.fontFamily shouldBe null
        spec.light.background shouldBe DefaultDarkColors.background.argb
    }

    @Test
    fun a_pill_button_is_rounder_than_any_button() {
        val shapes = PaymentShapes.Default.copy(button = PaymentCorners.Pill, sheet = PaymentCorners.Pill)

        val spec = ThemeSpec.of(PaymentTheme.Custom(colors = DefaultDarkColors, shapes = shapes))

        (spec.buttonCorner > 54.0) shouldBe true
        spec.sheetCorner shouldBe 26.0
    }
}
