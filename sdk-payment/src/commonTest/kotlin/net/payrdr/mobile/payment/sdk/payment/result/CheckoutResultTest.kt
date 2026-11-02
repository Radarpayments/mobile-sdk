package net.payrdr.mobile.payment.sdk.payment.result

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import kotlin.test.Test

class CheckoutResultTest {
    @Test fun `an unknown result closed by the system gets the order that was opened`() {
        CheckoutResult.Unknown("").withOrderId("md-1") shouldBe CheckoutResult.Unknown("md-1")
    }

    @Test fun `every variant without an order id gets it and keeps the rest`() {
        val error = SdkException("boom")
        CheckoutResult.Failed("", error).withOrderId("md-1") shouldBe CheckoutResult.Failed("md-1", error)
        CheckoutResult.Canceled("").withOrderId("md-1") shouldBe CheckoutResult.Canceled("md-1")
        CheckoutResult.Completed("").withOrderId("md-1") shouldBe CheckoutResult.Completed("md-1")
    }

    @Test fun `a result that already names its order keeps it`() {
        CheckoutResult.Completed("md-1").withOrderId("md-2") shouldBe CheckoutResult.Completed("md-1")
    }
}
