package net.payrdr.mobile.payment.samples.android.store.tickets

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistrar
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistration
import net.payrdr.mobile.payment.samples.android.store.LocalOrderRegistrarFactory
import net.payrdr.mobile.payment.samples.android.store.StoreViewModel
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class TicketsFlowTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var locale: Locale

    private fun show() =
        rule.setContent {
            locale = LocalConfiguration.current.locales[0]
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
                TicketsStore(store = StoreViewModel(), onBack = {}, onOutcome = { _, _ -> }, onOpenSettings = {})
            }
        }

    @Test fun twoBalconySeatsWithTheFeeArePaidOnCheckout() {
        show()
        rule.onNodeWithText(ctx.getString(R.string.ticket_balcony)).performClick()
        rule.onNode(hasText(ctx.getString(R.string.tickets_to_payment, formatEur(13000, locale)))).performClick()
        rule.onNode(hasText(ctx.getString(R.string.store_pay, formatEur(13350, locale)))).assertExists()
    }

    @Test fun quantityStopsAtSix() {
        show()
        repeat(9) { rule.onNode(hasContentDescription(ctx.getString(R.string.store_increase))).performClick() }
        rule.onNode(hasText(ctx.getString(R.string.tickets_to_payment, formatEur(6 * 6500L, locale)))).assertExists()
    }
}
