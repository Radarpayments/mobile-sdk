package net.payrdr.mobile.payment.samples.android.store.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import net.payrdr.mobile.payment.samples.android.store.invest.InvestColors
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentColor
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentCorners
import net.payrdr.mobile.payment.sdk.payment.ui.theme.toPaymentColor
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoverStyleTest {
    private fun contrast(
        a: Color,
        b: Color,
    ): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return max(la, lb) / min(la, lb)
    }

    @Test fun `furniture primary is 1C1917`() {
        assertEquals(Color(0xFF1C1917), coverStyle(CoverId.FURNITURE).primary)
    }

    @Test fun `only coffee and Northwind are dark`() {
        assertTrue(coverStyle(CoverId.COFFEE).dark)
        assertTrue(coverStyle(CoverId.INVEST).dark)
        assertFalse(coverStyle(CoverId.FURNITURE).dark)
        assertFalse(coverStyle(CoverId.TICKETS).dark)
    }

    @Test fun `paymentTheme uses cover primary and radius`() {
        val style = coverStyle(CoverId.TICKETS)
        val theme = style.paymentTheme()
        assertEquals(PaymentColor(0xFFEA580C), theme.colors.primary)
        assertEquals(PaymentColor(0xFF000000), theme.colors.onPrimary)
        assertEquals(PaymentCorners.Radius(14f), theme.shapes.field)
    }

    @Test fun `text on primary and on background is readable in every cover`() {
        CoverId.entries.map(::coverStyle).forEach { s ->
            assertTrue(contrast(s.primary, s.onPrimary) >= 4.5f, "${s.id} onPrimary")
            assertTrue(contrast(s.background, s.text) >= 4.5f, "${s.id} text")
            assertTrue(contrast(s.background, s.muted) >= 4.5f, "${s.id} muted")
        }
    }

    @Test fun `Northwind's sheet takes the invest tokens`() {
        val theme = coverStyle(CoverId.INVEST).paymentTheme()
        assertEquals(InvestColors.accent.toPaymentColor(), theme.colors.primary)
        assertEquals(InvestColors.onAccent.toPaymentColor(), theme.colors.onPrimary)
        assertEquals(InvestColors.surface.toPaymentColor(), theme.colors.background)
        assertEquals(InvestColors.surface2.toPaymentColor(), theme.colors.surface)
        assertEquals(InvestColors.text.toPaymentColor(), theme.colors.onBackground)
        assertEquals(InvestColors.muted.toPaymentColor(), theme.colors.onSurfaceVariant)
        assertEquals(InvestColors.fieldBorder.toPaymentColor(), theme.colors.outline)
        assertEquals(PaymentCorners.Radius(12f), theme.shapes.button)
        assertEquals(Color(0xFF22C55E), InvestColors.accent)
    }
}
