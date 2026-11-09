package net.payrdr.mobile.payment.samples.android.store.furniture

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistrar
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistration
import net.payrdr.mobile.payment.samples.android.store.LocalOrderRegistrarFactory
import net.payrdr.mobile.payment.samples.android.store.StoreViewModel
import net.payrdr.mobile.payment.samples.android.store.model.Catalog
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import org.junit.Rule
import org.junit.Test

class FurnitureFlowTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var locale: java.util.Locale
    private val sofa = Catalog.products(CoverId.FURNITURE).first { it.id == "velvet_sofa" }

    private fun show(store: StoreViewModel = StoreViewModel()) =
        rule.setContent {
            // The same locale the screens format prices with (the app may run in its own language).
            locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
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
                FurnitureStore(store = store, onBack = {}, onOutcome = { _, _ -> }, onOpenSettings = {})
            }
        }

    @Test fun addFromProductScreenShowsBadge() {
        show()
        rule.onNodeWithText("Velvet sofa").performScrollTo().performClick()
        rule.onNode(hasText(ctx.getString(R.string.store_add_to_cart, formatEur(sofa.priceMinor, locale)))).performClick()
        rule.onNode(hasContentDescription(ctx.getString(R.string.store_cart_count, 1))).assertExists()
    }

    @Test fun cartTotalUpdatesOnStepper() {
        val store = StoreViewModel().apply { add(CoverId.FURNITURE, sofa) }
        show(store)
        rule.onNode(hasContentDescription(ctx.getString(R.string.store_cart_count, 1))).performClick()
        rule.onNode(hasContentDescription(ctx.getString(R.string.store_increase))).performClick()
        rule.onNode(hasText(ctx.getString(R.string.store_pay, formatEur(2 * sofa.priceMinor, locale)))).assertExists()
    }

    @Test fun chipFilterShowsOnlyLamps() {
        show()
        rule.onNodeWithText(ctx.getString(R.string.furniture_light)).performClick()
        rule.onNodeWithText("Arc floor lamp").assertExists()
        rule.onNodeWithText("Velvet sofa").assertDoesNotExist()
    }

    @Test fun backFromProductKeepsTheChosenCategory() {
        show()
        rule.onNodeWithText(ctx.getString(R.string.furniture_light)).performClick()
        rule.onNodeWithText("Arc floor lamp").performClick()
        rule.onNode(hasContentDescription(ctx.getString(R.string.store_back))).performClick()
        rule.onNodeWithText("Arc floor lamp").assertExists()
        rule.onNodeWithText("Velvet sofa").assertDoesNotExist()
    }

    @Test fun missingCredentialsKeepsCartAndShowsError() {
        val store = StoreViewModel().apply { add(CoverId.FURNITURE, sofa) }
        show(store)
        rule.onNode(hasContentDescription(ctx.getString(R.string.store_cart_count, 1))).performClick()
        rule.onNode(hasText(ctx.getString(R.string.store_pay, formatEur(sofa.priceMinor, locale)))).performClick()
        rule.onNodeWithText(ctx.getString(R.string.store_error_credentials)).assertExists()
        rule.onNodeWithText("Velvet sofa").assertExists()
    }
}
