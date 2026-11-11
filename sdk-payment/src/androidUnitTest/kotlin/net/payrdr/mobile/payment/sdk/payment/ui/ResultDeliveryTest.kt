package net.payrdr.mobile.payment.sdk.payment.ui

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import kotlin.test.Test

class ResultDeliveryTest {
    private val results = mutableListOf<CheckoutResult>()

    @Test
    fun `a full-screen checkout hands the result over at once`() {
        val delivery = ResultDelivery(slidesAway = false, onResult = { results += it })

        delivery.deliver(CheckoutResult.Completed("order-1"))

        results shouldBe listOf(CheckoutResult.Completed("order-1"))
        delivery.delivered shouldBe true
    }

    @Test
    fun `the merchant gets one result per run whatever closes the checkout after it`() {
        val delivery = ResultDelivery(slidesAway = false, onResult = { results += it })

        delivery.deliver(CheckoutResult.Completed("order-1"))
        delivery.deliver(CheckoutResult.Canceled("order-1"))

        results shouldBe listOf(CheckoutResult.Completed("order-1"))
    }

    @Test
    fun `a sheet keeps the result until it has slid away`() {
        val delivery = ResultDelivery(slidesAway = true, onResult = { results += it })

        delivery.deliver(CheckoutResult.Completed("order-1"))
        delivery.handOver()

        results shouldBe emptyList()
        delivery.pending shouldBe CheckoutResult.Completed("order-1")
        delivery.shown.targetState shouldBe false
    }

    @Test
    fun `a second result while the sheet slides away does not replace the first`() {
        val delivery = ResultDelivery(slidesAway = true, onResult = { results += it })

        delivery.deliver(CheckoutResult.Completed("order-1"))
        delivery.deliver(CheckoutResult.Canceled("order-1"))

        delivery.pending shouldBe CheckoutResult.Completed("order-1")
    }
}
