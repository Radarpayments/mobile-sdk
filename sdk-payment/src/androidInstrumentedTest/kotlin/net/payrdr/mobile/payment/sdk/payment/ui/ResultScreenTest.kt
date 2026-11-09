package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentReceipt
import net.payrdr.mobile.payment.sdk.payment.ui.screens.ResultScreen
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentThemeProvider
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class ResultScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun success_renders_checkmark_receipt_and_done_invokes_callback() {
        var closed = false
        val result = CheckoutResult.Completed(orderId = "p1")

        composeRule.setContent {
            PaymentThemeProvider(theme = PaymentTheme.Light) {
                ResultScreen(
                    result = result,
                    receipt = PaymentReceipt(formattedAmount = "€49.90", panMasked = "411111**1111"),
                    onClose = { closed = true },
                )
            }
        }

        composeRule.onNodeWithTag("SuccessIcon", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Payment successful").assertIsDisplayed()
        composeRule.onNodeWithText(ltr("•••• 1111")).assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()
        assertTrue(closed, "onClose should have been invoked")
    }

    @Test
    fun failure_renders_error_icon_title_and_reason_without_the_sdk_message() {
        val result =
            CheckoutResult.Failed(
                orderId = "p1",
                error = SdkException("Something specific broke"),
            )

        composeRule.setContent {
            PaymentThemeProvider(theme = PaymentTheme.Light) {
                ResultScreen(
                    result = result,
                    receipt = PaymentReceipt(actionCodeDescription = "Insufficient funds"),
                    onClose = {},
                    onRetry = {},
                )
            }
        }

        composeRule.onNodeWithTag("FailureIcon", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Payment declined").assertIsDisplayed()
        composeRule.onNodeWithText("Insufficient funds").assertIsDisplayed()
        // No chip: the SDK's own message is for developers, not the buyer.
        composeRule.onNodeWithText("Something specific broke").assertDoesNotExist()
        composeRule.onNodeWithText("Pay with another card").assertIsDisplayed()
    }
}
