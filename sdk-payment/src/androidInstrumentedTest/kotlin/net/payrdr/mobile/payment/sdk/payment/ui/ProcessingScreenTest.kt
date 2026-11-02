package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.result.ProcessingStep
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ProcessingScreen
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentThemeProvider
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import org.junit.Rule
import org.junit.Test

class ProcessingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renders_title_amount_and_merchant() {
        composeRule.setContent {
            PaymentThemeProvider(theme = PaymentTheme.Light) {
                ProcessingScreen(
                    step = ProcessingStep.SUBMITTING_PAYMENT,
                    order = OrderSummary(formattedAmount = "€49.90"),
                )
            }
        }
        composeRule.onNodeWithTag("ProcessingScreen").assertIsDisplayed()
        composeRule.onNodeWithText("Processing payment").assertIsDisplayed()
        composeRule.onNodeWithText(ltr("€49.90")).assertIsDisplayed()
    }

    @Test
    fun announces_current_step() {
        composeRule.setContent {
            PaymentThemeProvider(theme = PaymentTheme.Light) {
                ProcessingScreen(step = ProcessingStep.TOKENIZING_CARD)
            }
        }
        composeRule.onNodeWithContentDescription("Encrypting card…").assertIsDisplayed()
    }

    @Test
    fun shows_the_current_step() {
        composeRule.setContent {
            PaymentThemeProvider(theme = PaymentTheme.Light) {
                ProcessingScreen(step = ProcessingStep.TOKENIZING_CARD)
            }
        }
        composeRule.onNodeWithTag("processing_step").assertTextEquals("Encrypting card…")
    }
}
