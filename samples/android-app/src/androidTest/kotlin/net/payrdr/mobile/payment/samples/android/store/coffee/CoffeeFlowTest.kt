package net.payrdr.mobile.payment.samples.android.store.coffee

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistrar
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistration
import net.payrdr.mobile.payment.samples.android.store.LocalOrderRegistrarFactory
import net.payrdr.mobile.payment.samples.android.store.StoreViewModel
import net.payrdr.mobile.payment.samples.android.store.model.Catalog
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import org.junit.Rule
import org.junit.Test

class CoffeeFlowTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val flatWhite = Catalog.products(CoverId.COFFEE).first { it.id == "flat_white" }

    private fun show(store: StoreViewModel = StoreViewModel()) =
        rule.setContent {
            CompositionLocalProvider(
                LocalOrderRegistrarFactory provides {
                    OrderRegistrar {
                            _,
                            _,
                        ->
                        OrderRegistration.MissingCredentials
                    }
                },
            ) {
                CoffeeStore(store = store, onBack = {}, onOutcome = { _, _ -> }, onOpenSettings = {})
            }
        }

    @Test fun repeatIsHiddenBeforeTheFirstOrder() {
        show()
        rule.onNodeWithText(ctx.getString(R.string.coffee_repeat)).assertDoesNotExist()
    }

    @Test fun plusAddsToThePill() {
        show()
        rule.onNode(hasContentDescription(ctx.getString(R.string.coffee_add_item, "Flat white"))).performClick()
        rule.onNode(hasText(ctx.getString(R.string.store_cart_count, 1))).assertExists()
    }

    @Test fun repeatAppearsAfterAPaidOrderAndRefillsTheOrder() {
        val store =
            StoreViewModel().apply {
                add(CoverId.COFFEE, flatWhite)
                onOutcome(CoverId.COFFEE, CheckoutOutcome.Success("p-1"), flatWhite.priceMinor)
            }
        show(store)
        rule.onNodeWithText(ctx.getString(R.string.coffee_repeat)).performClick()
        rule.onNodeWithText(ctx.getString(R.string.coffee_order_title)).assertExists()
    }

    @Test fun afterAPaidOrderTheMenuComesBackWithRepeat() {
        val store = StoreViewModel()
        show(store)
        rule.onNode(hasContentDescription(ctx.getString(R.string.coffee_add_item, "Flat white"))).performClick()
        rule.onNode(hasText(ctx.getString(R.string.store_cart_count, 1))).performClick()
        rule.onNodeWithText(ctx.getString(R.string.coffee_order_title)).assertExists()
        rule.runOnIdle { store.onOutcome(CoverId.COFFEE, CheckoutOutcome.Success("p-1"), flatWhite.priceMinor) }
        rule.onNodeWithText(ctx.getString(R.string.coffee_repeat)).assertExists()
    }
}
