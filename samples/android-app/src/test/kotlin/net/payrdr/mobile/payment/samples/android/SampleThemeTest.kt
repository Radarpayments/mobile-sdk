package net.payrdr.mobile.payment.samples.android

import androidx.compose.ui.graphics.Color
import net.payrdr.mobile.payment.samples.android.ui.components.BrandColors
import net.payrdr.mobile.payment.samples.android.ui.components.BrandGradients
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleThemeTest {
    @Test fun `brand primary indigo is fixed hex`() {
        assertEquals(Color(0xFF6366F1), BrandColors.Indigo)
    }

    @Test fun `brand primary violet is fixed hex`() {
        assertEquals(Color(0xFF8B5CF6), BrandColors.Violet)
    }

    @Test fun `category gradient emerald has two stops`() {
        val grad = BrandGradients.Emerald
        assertEquals(2, grad.colorStops.size)
    }
}
