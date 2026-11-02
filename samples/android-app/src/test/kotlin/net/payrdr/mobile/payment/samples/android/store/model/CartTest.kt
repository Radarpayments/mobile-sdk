package net.payrdr.mobile.payment.samples.android.store.model

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CartTest {
    private val sofa = Product(id = "sofa", name = 0, subtitle = null, priceMinor = 124900, image = null)
    private val chair = Product(id = "chair", name = 0, subtitle = null, priceMinor = 32900, image = null)

    @Test fun `add twice increments quantity`() {
        val cart = Cart().add(sofa).add(sofa)
        assertEquals(listOf(CartLine(sofa, 2)), cart.lines)
        assertEquals(2, cart.count)
    }

    @Test fun `total of sofa and chair is 157800`() {
        assertEquals(157800, Cart().add(sofa).add(chair).totalMinor)
    }

    @Test fun `setQuantity 0 removes line`() {
        val cart = Cart().add(sofa).add(chair).setQuantity("sofa", 0)
        assertEquals(listOf(CartLine(chair, 1)), cart.lines)
    }

    @Test fun `setQuantity changes total`() {
        assertEquals(249800, Cart().add(sofa).setQuantity("sofa", 2).totalMinor)
    }

    @Test fun `empty cart isEmpty and total 0`() {
        val cart = Cart().add(sofa).clear()
        assertTrue(cart.isEmpty)
        assertEquals(0, cart.totalMinor)
    }

    @Test fun `formatEur in English`() {
        assertEquals("€1,578.00", formatEur(157800, Locale.ENGLISH))
    }

    @Test fun `formatEur in Russian`() {
        assertEquals("1 578,00 €", formatEur(157800, Locale("ru")).replace(' ', ' ').replace(' ', ' '))
    }
}
