package net.payrdr.mobile.payment.samples.android.store.tickets

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.store.StoreViewModel
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.theme.CoverTheme

/** Stage Pass: the event with seat categories → checkout with a seat hold → the shared payment bar. */
@Composable
fun TicketsStore(
    store: StoreViewModel,
    onBack: () -> Unit,
    onOutcome: (CheckoutOutcome, Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val carts by store.allCarts.collectAsState()
    var categoryId by rememberSaveable { mutableStateOf("balcony") }
    var quantity by rememberSaveable { mutableStateOf(2) }
    var checkout by rememberSaveable { mutableStateOf(false) }
    var holdExpired by rememberSaveable { mutableStateOf(false) }
    val order = TicketOrder(categoryId, quantity)
    // After a paid order the cart is empty: the buyer is back at the event.
    val showCheckout = checkout && !carts.getValue(CoverId.TICKETS).isEmpty
    BackHandler(enabled = showCheckout) { checkout = false }
    CoverTheme(CoverId.TICKETS) {
        AnimatedContent(
            targetState = showCheckout,
            transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) },
            label = "tickets",
        ) { onCheckout ->
            if (onCheckout) {
                TicketsCheckoutScreen(
                    order = order,
                    onBack = { checkout = false },
                    onHoldExpired = {
                        holdExpired = true
                        checkout = false
                    },
                    onOutcome = onOutcome,
                    onOpenSettings = onOpenSettings,
                )
            } else {
                TicketsEventScreen(
                    order = order,
                    holdExpired = holdExpired,
                    onBack = onBack,
                    onCategory = { categoryId = it },
                    onQuantity = { quantity = order.withQuantity(it).quantity },
                    onNext = {
                        holdExpired = false
                        store.replaceCart(CoverId.TICKETS, order.toCart())
                        checkout = true
                    },
                )
            }
        }
    }
}

private const val FADE_MS = 180
