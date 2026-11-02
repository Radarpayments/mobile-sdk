package net.payrdr.mobile.payment.samples.android.store.tickets

import net.payrdr.mobile.payment.samples.android.store.model.Cart
import net.payrdr.mobile.payment.samples.android.store.model.CartLine
import net.payrdr.mobile.payment.samples.android.store.model.Catalog
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.TICKET_FEE_ID

/** Seats picked for the concert: a category, how many (1–6), plus one service fee per order. */
data class TicketOrder(
    val categoryId: String = "balcony",
    val quantity: Int = 2,
) {
    /** Tickets and the fee, in euro cents. */
    val totalMinor: Long get() = toCart().totalMinor

    /** The same order with [quantity] kept between 1 and [MAX_TICKETS]. */
    fun withQuantity(quantity: Int): TicketOrder = copy(quantity = quantity.coerceIn(1, MAX_TICKETS))

    /** Cart lines the store pays for: the seats and the fee. */
    fun toCart(): Cart {
        val products = Catalog.products(CoverId.TICKETS)
        return Cart(
            listOf(
                CartLine(products.first { it.id == categoryId }, quantity),
                CartLine(products.first { it.id == TICKET_FEE_ID }, 1),
            ),
        )
    }

    /** Limits of an order. */
    companion object {
        /** Most seats one order can hold. */
        const val MAX_TICKETS = 6
    }
}
