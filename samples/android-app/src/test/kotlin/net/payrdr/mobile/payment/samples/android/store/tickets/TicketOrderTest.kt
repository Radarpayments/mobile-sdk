package net.payrdr.mobile.payment.samples.android.store.tickets

import kotlin.test.Test
import kotlin.test.assertEquals

class TicketOrderTest {
    @Test fun `two balcony seats with the fee are 133_50`() {
        assertEquals(13350, TicketOrder(categoryId = "balcony", quantity = 2).totalMinor)
    }

    @Test fun `quantity stays between 1 and 6`() {
        assertEquals(6, TicketOrder(quantity = 6).withQuantity(9).quantity)
        assertEquals(1, TicketOrder(quantity = 1).withQuantity(0).quantity)
    }

    @Test fun `the order becomes cart lines with the fee once`() {
        val cart = TicketOrder(categoryId = "floor", quantity = 3).toCart()
        assertEquals(listOf("floor" to 3, "fee" to 1), cart.lines.map { it.product.id to it.quantity })
        assertEquals(3 * 4500L + 350, cart.totalMinor)
    }
}
