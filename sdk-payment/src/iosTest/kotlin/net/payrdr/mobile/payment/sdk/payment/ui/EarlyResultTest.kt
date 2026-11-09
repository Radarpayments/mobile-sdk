package net.payrdr.mobile.payment.sdk.payment.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkInvalidSessionIdException
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.support.FakeKeyProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakePaymentApi
import net.payrdr.mobile.payment.sdk.payment.support.FakeWebChallengeHost
import kotlin.test.Test
import kotlin.test.assertEquals

/** The hosts start the run before the checkout first composes, so the run may already have ended by then. */
@OptIn(ExperimentalTestApi::class)
class EarlyResultTest {
    @Test
    fun a_checkout_that_ended_before_it_was_shown_still_hands_over_its_result() =
        runComposeUiTest {
            val controller =
                PaymentFlowController(
                    api = FakePaymentApi(),
                    keyProvider = FakeKeyProvider(),
                    core = SdkCore(),
                    webChallengeHost = FakeWebChallengeHost(),
                    logger = Logger(),
                    scope = CoroutineScope(Dispatchers.Unconfined),
                )
            controller.handle(PaymentIntent.Start(CheckoutConfig.SessionId("not-a-session")))
            val results = mutableListOf<CheckoutResult>()

            setContent {
                PaymentCheckout(
                    SdkPaymentConfig(baseUrl = "https://dev.bpcbt.com/payment"),
                    controller,
                    onResult = { results += it },
                )
            }
            waitForIdle()

            assertEquals(1, results.size)
            results
                .single()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkInvalidSessionIdException>()
        }
}
