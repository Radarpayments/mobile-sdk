package net.payrdr.mobile.payment.samples.android.store.model

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** One product and how many of it the buyer takes. */
data class CartLine(
    val product: Product,
    val quantity: Int,
)

/** Immutable shopping cart: every change returns a new cart. Prices are euro cents. */
data class Cart(
    val lines: List<CartLine> = emptyList(),
) {
    /** Sum of all lines in euro cents. */
    val totalMinor: Long get() = lines.sumOf { it.product.priceMinor * it.quantity }

    /** Number of items, counting quantities. */
    val count: Int get() = lines.sumOf { it.quantity }

    /** True when there is nothing to pay for. */
    val isEmpty: Boolean get() = lines.isEmpty()

    /** Adds one [product]. */
    fun add(product: Product): Cart {
        val current = lines.find { it.product.id == product.id }?.quantity ?: 0
        return if (current == 0) copy(lines = lines + CartLine(product, 1)) else setQuantity(product.id, current + 1)
    }

    /** Sets the quantity of [productId]; 0 or less removes the line. */
    fun setQuantity(
        productId: String,
        quantity: Int,
    ): Cart =
        copy(
            lines =
                if (quantity <= 0) {
                    lines.filterNot { it.product.id == productId }
                } else {
                    lines.map { if (it.product.id == productId) it.copy(quantity = quantity) else it }
                },
        )

    /** Empties the cart. */
    fun clear(): Cart = Cart()
}

/** Formats euro [minor] units for [locale], e.g. "€1,578.00" or "1 578,00 €". */
fun formatEur(
    minor: Long,
    locale: Locale,
): String =
    NumberFormat
        .getCurrencyInstance(locale)
        .apply { currency = Currency.getInstance("EUR") }
        .format(minor / CENTS)

private const val CENTS = 100.0
