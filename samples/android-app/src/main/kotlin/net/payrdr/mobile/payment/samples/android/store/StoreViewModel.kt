package net.payrdr.mobile.payment.samples.android.store

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.store.model.Cart
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Product

/** Carts of the demo businesses and the result of the last payment, shared by all store screens. */
class StoreViewModel : ViewModel() {
    /** What the store's result screen shows. */
    data class Receipt(
        val outcome: CheckoutOutcome,
        val amountMinor: Long,
    )

    private val carts = MutableStateFlow(CoverId.entries.associateWith { Cart() })
    private val receipt = MutableStateFlow<Receipt?>(null)
    private val paid = MutableStateFlow<Map<CoverId, Cart>>(emptyMap())
    private val orderNumber = MutableStateFlow(FIRST_ORDER_NUMBER - 1)

    /** Carts by cover; observe it to redraw on changes. */
    val allCarts: StateFlow<Map<CoverId, Cart>> = carts.asStateFlow()

    /** The last payment that ended in success or decline; null after a cancel. */
    val lastReceipt: StateFlow<Receipt?> = receipt.asStateFlow()

    /** Number of the last paid order, as the coffee shop calls it out. */
    val lastOrderNumber: StateFlow<Int> = orderNumber.asStateFlow()

    /** Paid orders by cover, observe it to redraw on changes. */
    val allPaid: StateFlow<Map<CoverId, Cart>> = paid.asStateFlow()

    /** The last paid order of [cover], or null when nothing was paid there yet. */
    fun lastPaid(cover: CoverId): Cart? = paid.value[cover]

    /** Puts the last paid order of [cover] back into its cart, for "Order again". */
    fun repeatLast(cover: CoverId) {
        val last = lastPaid(cover) ?: return
        edit(cover) { last }
    }

    fun cart(cover: CoverId): Cart = carts.value.getValue(cover)

    /** Adds one [product] to the cart of [cover]. */
    fun add(
        cover: CoverId,
        product: Product,
    ) = edit(cover) { it.add(product) }

    /** Sets how many of [productId] are in the cart of [cover]. */
    fun setQuantity(
        cover: CoverId,
        productId: String,
        quantity: Int,
    ) = edit(cover) { it.setQuantity(productId, quantity) }

    /** Replaces the cart of [cover] with [cart], e.g. the seats picked for a concert. */
    fun replaceCart(
        cover: CoverId,
        cart: Cart,
    ) = edit(cover) { cart }

    /** Records how the payment of [amountMinor] for [cover] ended: a success empties the cart. */
    fun onOutcome(
        cover: CoverId,
        outcome: CheckoutOutcome,
        amountMinor: Long,
    ) {
        receipt.value = if (outcome is CheckoutOutcome.Cancelled) null else Receipt(outcome, amountMinor)
        if (outcome is CheckoutOutcome.Success) {
            paid.update { it + (cover to cart(cover)) }
            orderNumber.update { it + 1 }
            edit(cover) { it.clear() }
        }
    }

    private fun edit(
        cover: CoverId,
        change: (Cart) -> Cart,
    ) = carts.update { it + (cover to change(it.getValue(cover))) }
}

private const val FIRST_ORDER_NUMBER = 42
