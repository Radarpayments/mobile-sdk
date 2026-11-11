package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.presentation.SkeletonLayout
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.screens.PaymentMethodsContent
import net.payrdr.mobile.payment.sdk.payment.ui.screens.PaymentMethodsSkeleton
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentThemeProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CheckoutSkeletonTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun cards(count: Int) =
        List(count) {
            SavedCardInfo("b$it", "411111**111$it", "VISA", "JOHN", canDelete = false, expiry = "08/28")
        }

    /** One column laid out as the payment sheet lays out its first step, reporting its height. */
    @Composable
    private fun Measured(
        onHeight: (Int) -> Unit,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).onSizeChanged { onHeight(it.height) },
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }

    @Test
    fun skeleton_is_as_tall_as_the_payment_sheet() {
        val skeleton = IntArray(4)
        val content = IntArray(4)
        composeRule.setContent {
            PaymentThemeProvider(theme = PaymentTheme.Light) {
                // Overlaid, so each column gets the whole screen to lay out in.
                Box {
                    for (count in 1..3) {
                        Measured({ skeleton[count] = it }) {
                            PaymentMethodsSkeleton(SkeletonLayout(wallet = false, savedCards = count), visible = true, onClose = {})
                        }
                        Measured({ content[count] = it }) {
                            PaymentMethodsContent(
                                wallets = WalletAvailability.None,
                                order = null,
                                savedCards = cards(count),
                                onClose = {},
                                onGooglePay = {},
                                onApplePay = {},
                                onPaySaved = {},
                                onNewCard = {},
                                onAllCards = {},
                                // As the skeleton draws it: no saved card leads to a card step of its own.
                                opensCardStep = { false },
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        for (count in 1..3) assertEquals("$count saved cards", content[count], skeleton[count])
    }
}
