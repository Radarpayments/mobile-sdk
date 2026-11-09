package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.CardFormScreen
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentThemeProvider
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Instrumented test for [CardFormScreen]:
 *   - Renders the three card fields and the Pay button (with the amount) under PaymentThemeProvider.
 *   - Pay stays tappable; with empty fields it shows field errors and dispatches nothing.
 *   - Tapping Pay with valid fields fires a `SubmitNewCard` intent with the expected raw values.
 */
class CardFormScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val config = SdkPaymentConfig(baseUrl = "https://example.test")

    @Test
    fun renders_fields_and_pay_with_amount() {
        composeRule.setContent {
            PaymentThemeProvider(theme = config.theme) {
                CardFormScreen(config = config, onSubmit = { }, order = OrderSummary(formattedAmount = "€49.90"))
            }
        }

        // English labels (default locale in tests):
        composeRule.onNodeWithText("Card number").assertIsDisplayed()
        composeRule.onNodeWithText("Expiry date").assertIsDisplayed()
        composeRule.onNodeWithText("CVC/CVV").assertIsDisplayed()
        composeRule.onNodeWithText("Pay ${ltr("€49.90")}").assertIsDisplayed()
    }

    @Test
    fun pay_with_empty_fields_shows_errors_and_does_not_submit() {
        var captured: PaymentIntent.SubmitNewCard? = null
        composeRule.setContent {
            PaymentThemeProvider(theme = config.theme) {
                CardFormScreen(config = config, onSubmit = { captured = it })
            }
        }

        composeRule.onNodeWithText("Pay").performClick()

        // Every error of the group shows, one per line under the fields.
        composeRule.onNodeWithText("Enter the card number", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Enter the date as MM/\u2060YY", substring = true).assertIsDisplayed()
        assertNull(captured, "Nothing should be dispatched while fields are invalid")
    }

    @Test
    fun entering_valid_fields_and_tapping_pay_fires_intent() {
        var captured: PaymentIntent.SubmitNewCard? = null

        composeRule.setContent {
            PaymentThemeProvider(theme = config.theme) {
                CardFormScreen(config = config, onSubmit = { captured = it }, canSaveCard = false)
            }
        }

        // Valid luhn-checked Visa test PAN
        composeRule.onNode(hasSetTextAction() and hasText("Card number")).performTextInput("4111111111111111")
        composeRule.onNode(hasSetTextAction() and hasText("Expiry date")).performTextInput("1299")
        composeRule.onNode(hasSetTextAction() and hasText("CVC/CVV")).performTextInput("123")
        // Pay sits under the fields once the keyboard is closed.
        Espresso.closeSoftKeyboard()

        composeRule.onNodeWithText("Pay").performClick()

        assertNotNull(captured, "SubmitNewCard should have been dispatched")
        assertEquals("4111111111111111", captured!!.pan)
        assertEquals("12/99", captured!!.expiryMMYY)
        assertEquals("123", captured!!.cvc)
        assertEquals(false, captured!!.saveCard)
    }
}
