package net.payrdr.mobile.payment.samples.android.store

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistrar
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistration
import net.payrdr.mobile.payment.samples.android.prefs.AppPrefs
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Scenario
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class StoreCheckoutBarTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val noCredentials = OrderRegistrar { _, _ -> OrderRegistration.MissingCredentials }

    private fun show(
        amountMinor: Long,
        onOpenSettings: () -> Unit = {},
    ) = rule
        .setContent {
            CompositionLocalProvider(LocalOrderRegistrarFactory provides { noCredentials }) {
                StoreCheckoutBar(cover = CoverId.FURNITURE, amountMinor = amountMinor, onOutcome = {}, onOpenSettings = onOpenSettings)
            }
        }.also {
            // The bar appears once DataStore has loaded the settings.
            rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag(PAY_BUTTON_TAG)).fetchSemanticsNodes().isNotEmpty() }
        }

    @Test fun missingCredentialsShowsErrorAndSettingsLink() {
        var settings = false
        show(157800) { settings = true }
        rule.onNodeWithTag(PAY_BUTTON_TAG).performClick()
        rule.onNodeWithText(ctx.getString(R.string.store_error_credentials)).assertExists()
        rule.onNodeWithText(ctx.getString(R.string.store_open_settings)).performClick()
        assertTrue(settings)
    }

    @Test fun emptyCartCannotPay() {
        show(0)
        rule.onNodeWithTag(PAY_BUTTON_TAG).assertIsNotEnabled()
    }

    @Test fun scenarioToggleIsSaved() {
        runBlocking { AppPrefs(ctx).setScenario(Scenario(buyerWithCards = false)) }
        show(157800)
        rule.onNodeWithTag(SCENARIO_ROW_TAG).performClick()
        rule.onNodeWithText(ctx.getString(R.string.scenario_with_cards)).performClick()
        rule.waitUntil(5_000) {
            runBlocking {
                AppPrefs(ctx)
                    .flow
                    .first()
                    .scenario.buyerWithCards
            }
        }
        rule.onNodeWithTag(SCENARIO_CARDS_TAG).assertIsSelected()
    }
}
