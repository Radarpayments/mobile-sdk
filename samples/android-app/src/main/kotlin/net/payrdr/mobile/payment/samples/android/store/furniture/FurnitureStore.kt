package net.payrdr.mobile.payment.samples.android.store.furniture

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.store.StoreViewModel
import net.payrdr.mobile.payment.samples.android.store.model.Catalog
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Product
import net.payrdr.mobile.payment.samples.android.store.theme.CoverTheme

/** Lets a product photo fly between its catalogue tile and the top of the product page. */
typealias PhotoFlight = @Composable Modifier.(Product) -> Modifier

/** Hearth & Co: catalogue → product → cart → the shared payment bar. */
@Composable
fun FurnitureStore(
    store: StoreViewModel,
    onBack: () -> Unit,
    onOutcome: (CheckoutOutcome, Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val carts by store.allCarts.collectAsState()
    val cart = carts.getValue(CoverId.FURNITURE)
    // "catalog", "cart" or a product id.
    var screen by rememberSaveable { mutableStateOf(CATALOG) }
    // An empty cart is never shown: after a paid order the buyer is back at the catalogue.
    val shown = if (screen == CART && cart.isEmpty) CATALOG else screen
    BackHandler(enabled = shown != CATALOG) { screen = CATALOG }
    // Kept here, not in the catalogue: back from a product the grid is where it was, so the photo has a tile to land on.
    val grid = rememberLazyGridState()
    var category by rememberSaveable { mutableStateOf(FurnitureCategory.ALL) }
    CoverTheme(CoverId.FURNITURE) {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = shown,
                transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) },
                label = "furniture",
            ) { current ->
                val flight: PhotoFlight = { product ->
                    sharedElement(rememberSharedContentState("photo-${product.id}"), this@AnimatedContent)
                }
                when (current) {
                    CATALOG ->
                        FurnitureCatalogScreen(
                            cart = cart,
                            onBack = onBack,
                            onProduct = { screen = it.id },
                            onCart = { screen = CART },
                            grid = grid,
                            category = category,
                            onCategory = { category = it },
                            flight = flight,
                        )
                    CART ->
                        FurnitureCartScreen(
                            cart = cart,
                            onBack = { screen = CATALOG },
                            onQuantity = { id, qty -> store.setQuantity(CoverId.FURNITURE, id, qty) },
                            onOutcome = onOutcome,
                            onOpenSettings = onOpenSettings,
                        )
                    else ->
                        FurnitureProductScreen(
                            product = Catalog.products(CoverId.FURNITURE).first { it.id == current },
                            onBack = { screen = CATALOG },
                            onAdd = {
                                store.add(CoverId.FURNITURE, it)
                                screen = CATALOG
                            },
                            flight = flight,
                        )
                }
            }
        }
    }
}

private const val CATALOG = "catalog"
private const val CART = "cart"
private const val FADE_MS = 240
