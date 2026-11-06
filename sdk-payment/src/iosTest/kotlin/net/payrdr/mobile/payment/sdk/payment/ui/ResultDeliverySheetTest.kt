package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import kotlin.test.Test

/** The sheet's result goes out only when its slide has ended: the host removes the controller as soon as it gets it. */
@OptIn(ExperimentalTestApi::class)
class ResultDeliverySheetTest {
    @Test
    fun the_sheet_hands_the_result_over_once_it_has_slid_away() =
        runComposeUiTest {
            val results = mutableListOf<CheckoutResult>()
            lateinit var delivery: ResultDelivery
            setContent {
                delivery = rememberResultDelivery(CheckoutPresentation.Sheet) { results += it }
                AnimatedVisibility(visibleState = delivery.shown) { Box(Modifier.size(100.dp)) }
            }

            mainClock.autoAdvance = false
            runOnIdle { delivery.deliver(CheckoutResult.Completed("order-1")) }
            mainClock.advanceTimeByFrame()
            results shouldBe emptyList()

            mainClock.autoAdvance = true
            waitForIdle()
            results shouldBe listOf(CheckoutResult.Completed("order-1"))
        }

    @Test
    fun a_close_during_the_slide_does_not_hand_over_a_second_result() =
        runComposeUiTest {
            val results = mutableListOf<CheckoutResult>()
            lateinit var delivery: ResultDelivery
            setContent {
                delivery = rememberResultDelivery(CheckoutPresentation.Sheet) { results += it }
                AnimatedVisibility(visibleState = delivery.shown) { Box(Modifier.size(100.dp)) }
            }

            runOnIdle {
                delivery.deliver(CheckoutResult.Completed("order-1"))
                delivery.deliver(CheckoutResult.Canceled("order-1"))
            }
            waitForIdle()
            runOnIdle { delivery.deliver(CheckoutResult.Canceled("order-1")) }
            waitForIdle()

            results shouldBe listOf(CheckoutResult.Completed("order-1"))
        }
}
