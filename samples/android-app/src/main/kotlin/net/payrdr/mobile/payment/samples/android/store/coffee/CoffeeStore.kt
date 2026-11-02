package net.payrdr.mobile.payment.samples.android.store.coffee

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

/** Roast Lane: menu → order → the shared payment bar; "Order again" pays the last order right away. */
@Composable
fun CoffeeStore(
    store: StoreViewModel,
    onBack: () -> Unit,
    onOutcome: (CheckoutOutcome, Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val carts by store.allCarts.collectAsState()
    val paid by store.allPaid.collectAsState()
    val cart = carts.getValue(CoverId.COFFEE)
    var onOrder by rememberSaveable { mutableStateOf(false) }
    var payNow by rememberSaveable { mutableStateOf(false) }
    // An empty order is never shown: after a paid order the buyer is back at the menu, with "Order again".
    val showOrder = onOrder && !cart.isEmpty
    BackHandler(enabled = showOrder) { onOrder = false }
    CoverTheme(CoverId.COFFEE) {
        AnimatedContent(
            targetState = showOrder,
            transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) },
            label = "coffee",
        ) { order ->
            if (order) {
                CoffeeOrderScreen(
                    cart = cart,
                    autoPay = payNow,
                    onAutoPayStarted = { payNow = false },
                    onBack = { onOrder = false },
                    onQuantity = { id, qty -> store.setQuantity(CoverId.COFFEE, id, qty) },
                    onOutcome = onOutcome,
                    onOpenSettings = onOpenSettings,
                )
            } else {
                CoffeeMenuScreen(
                    cart = cart,
                    lastPaid = paid[CoverId.COFFEE],
                    onBack = onBack,
                    onAdd = { store.add(CoverId.COFFEE, it) },
                    onRepeat = {
                        store.repeatLast(CoverId.COFFEE)
                        payNow = true
                        onOrder = true
                    },
                    onOrder = { onOrder = true },
                )
            }
        }
    }
}

private const val FADE_MS = 180
