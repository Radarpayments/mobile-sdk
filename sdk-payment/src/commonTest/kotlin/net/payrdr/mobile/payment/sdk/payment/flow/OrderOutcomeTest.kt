package net.payrdr.mobile.payment.sdk.payment.flow

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.FinishedPaymentInfoResponse
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkOrderNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionNotFoundException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import kotlin.test.Test

class OrderOutcomeTest {
    private val outcome = OrderOutcome(orderId = "ORDER-1", mdOrder = "ORDER-1", sessionFeatures = emptyList())

    @Test fun `a deposited order is completed`() {
        outcome.settled(FinishedPaymentInfoResponse(status = "DEPOSITED", actionCode = 0)) shouldBe CheckoutResult.Completed("ORDER-1")
    }

    @Test fun `no attempt yet and an outcome the gateway does not know yet settle nothing`() {
        outcome.settled(FinishedPaymentInfoResponse(status = "CREATED", actionCode = -100)).shouldBeNull()
        outcome
            .settled(
                FinishedPaymentInfoResponse(status = "CREATED", actionCode = 998, actionCodeDescription = "Timed out"),
            ).shouldBeNull()
    }

    @Test fun `a decline carries the payer's wording and the gateway's category`() {
        val result =
            outcome.settled(
                FinishedPaymentInfoResponse(
                    status = "DECLINED",
                    actionCode = 116,
                    displayErrorMessage = "Insufficient funds",
                    errorTypeName = "INSUFFICIENT_FUNDS",
                ),
            )
        val error = result.shouldBeInstanceOf<CheckoutResult.Failed>().error.shouldBeInstanceOf<SdkDeclinedException>()
        error.message shouldBe "Insufficient funds"
        error.declineCode shouldBe "INSUFFICIENT_FUNDS"
    }

    @Test fun `a card-saving order reversed with code 0 is completed - by its own features or the session's`() {
        val reversed = FinishedPaymentInfoResponse(status = "REVERSED", actionCode = 0)
        outcome.settled(reversed.copy(orderFeatures = listOf("VERIFY"))) shouldBe CheckoutResult.Completed("ORDER-1")
        OrderOutcome("ORDER-1", "ORDER-1", sessionFeatures = listOf("VERIFY")).settled(reversed) shouldBe
            CheckoutResult.Completed("ORDER-1")
        outcome.settled(reversed).shouldBeInstanceOf<CheckoutResult.Failed>()
    }

    @Test fun `a closed session the gateway does not know is not found - as the session or the order it was opened with`() {
        val unknown = FinishedPaymentInfoResponse()
        outcome
            .ofClosedSession(unknown, CheckoutConfig.SessionId("ps_1"))
            .shouldBeInstanceOf<CheckoutResult.Failed>()
            .error
            .shouldBeInstanceOf<SdkSessionNotFoundException>()
        outcome
            .ofClosedSession(unknown, CheckoutConfig.MdOrder("ORDER-1"))
            .shouldBeInstanceOf<CheckoutResult.Failed>()
            .error
            .shouldBeInstanceOf<SdkOrderNotFoundException>()
    }

    @Test fun `a card-saving receipt shows no amount`() {
        val info =
            FinishedPaymentInfoResponse(status = "REVERSED", actionCode = 0, formattedAmount = "0.00 EUR", orderFeatures = listOf("VERIFY"))
        outcome.receipt(info, currency = "EUR", language = null, orderAmount = "€0.00").formattedAmount.shouldBeNull()
    }
}
