package net.payrdr.mobile.payment.samples.android.store.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogTest {
    private fun price(
        cover: CoverId,
        id: String,
    ) = Catalog.products(cover).single { it.id == id }.priceMinor

    @Test fun `furniture prices match the mockups`() {
        assertEquals(124900, price(CoverId.FURNITURE, "velvet_sofa"))
        assertEquals(32900, price(CoverId.FURNITURE, "cloud_chair"))
        assertEquals(6, Catalog.products(CoverId.FURNITURE).size)
    }

    @Test fun `coffee has five items`() {
        assertEquals(5, Catalog.products(CoverId.COFFEE).size)
        assertEquals(390, price(CoverId.COFFEE, "flat_white"))
    }

    @Test fun `tickets are floor balcony and fee`() {
        assertEquals(listOf(4500L, 6500L, 350L), Catalog.products(CoverId.TICKETS).map { it.priceMinor })
    }
}
