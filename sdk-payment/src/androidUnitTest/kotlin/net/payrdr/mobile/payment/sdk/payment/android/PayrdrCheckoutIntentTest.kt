package net.payrdr.mobile.payment.sdk.payment.android

import android.app.Activity
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.RestorableCheckout
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import kotlin.test.Test

class PayrdrCheckoutIntentTest {
    private val config = SdkPaymentConfig(baseUrl = "https://pay.example.com", locale = "de")

    @Test fun `a launch waits in the registry with the order and the config`() {
        val run = PayrdrCheckoutIntent.run(config, CheckoutConfig.MdOrder("md-1"))

        val spec = SdkPaymentRegistry.take(run.runId)!!
        spec.checkout shouldBe CheckoutConfig.MdOrder("md-1")
        spec.sdk.config shouldBe config
        run.restorable shouldBe RestorableCheckout.encode(config, CheckoutConfig.MdOrder("md-1"))
    }

    @Test fun `every launch is a run of its own`() {
        val first = PayrdrCheckoutIntent.run(config, CheckoutConfig.MdOrder("md-1"))
        val second = PayrdrCheckoutIntent.run(config, CheckoutConfig.MdOrder("md-1"))

        (first.runId == second.runId) shouldBe false
        SdkPaymentRegistry.take(first.runId)
        SdkPaymentRegistry.take(second.runId)
    }

    @Test fun `a sheet closed without a result is unknown for the order that was opened`() {
        val result = PayrdrCheckoutIntent.parseResult(Activity.RESULT_CANCELED, data = null, mdOrder = "md-1")

        result.shouldBeInstanceOf<CheckoutResult.Unknown>()
        result.orderId shouldBe "md-1"
    }
}
