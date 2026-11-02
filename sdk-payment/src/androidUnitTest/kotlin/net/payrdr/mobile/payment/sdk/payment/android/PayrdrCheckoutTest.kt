package net.payrdr.mobile.payment.sdk.payment.android

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import kotlin.test.AfterTest
import kotlin.test.Test

class PayrdrCheckoutTest {
    private val config = SdkPaymentConfig(baseUrl = "https://pay.example.com")

    /** A screen that records launches and lets the test deliver the activity result. */
    private class FakeHost(
        state: Lifecycle.State = Lifecycle.State.CREATED,
    ) : ActivityResultCaller,
        LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = state }
        override val lifecycle: Lifecycle get() = registry
        val launches = mutableListOf<Any?>()
        var launchError: RuntimeException? = null
        private var callback: ActivityResultCallback<Any?>? = null

        fun deliver(result: CheckoutResult) = callback!!.onActivityResult(result)

        @Suppress("UNCHECKED_CAST")
        override fun <I, O> registerForActivityResult(
            contract: ActivityResultContract<I, O>,
            callback: ActivityResultCallback<O>,
        ): ActivityResultLauncher<I> {
            this.callback = callback as ActivityResultCallback<Any?>
            return object : ActivityResultLauncher<I>() {
                override val contract: ActivityResultContract<I, *> get() = contract

                override fun launch(
                    input: I,
                    options: ActivityOptionsCompat?,
                ) {
                    launchError?.let { throw it }
                    launches += input
                }

                override fun unregister() = Unit
            }
        }

        override fun <I, O> registerForActivityResult(
            contract: ActivityResultContract<I, O>,
            registry: ActivityResultRegistry,
            callback: ActivityResultCallback<O>,
        ): ActivityResultLauncher<I> = registerForActivityResult(contract, callback)
    }

    @AfterTest fun forgetRuns() {
        launchedRuns.forEach { SdkPaymentRegistry.take(it) }
        launchedRuns.clear()
    }

    private val launchedRuns = mutableListOf<String>()

    private fun FakeHost.runs(): List<String> = launches.map { (it as SdkPaymentRun).runId }.also { launchedRuns += it }

    @Test fun `a new config takes effect on the next present and the launched run carries it`() {
        val host = FakeHost()
        var current = SdkPayment.create(config)
        val checkout = PayrdrCheckout(sdk = { current }, onResult = { {} }, presented = InMemoryPresented())
        checkout.launcher = host.registerForActivityResult(SdkPaymentContract) { checkout.deliver(it) }
        val updated = SdkPayment.create(config.copy(locale = "de"))
        current = updated

        checkout.present("md-1")

        val runId = host.runs().single()
        SdkPaymentRegistry.take(runId)!!.sdk shouldBe updated
    }

    @Test fun `an instance recreated with the screen still knows the open checkout`() {
        val presented = InMemoryPresented()
        val host = FakeHost()
        val first = PayrdrCheckout(sdk = { SdkPayment.create(config) }, onResult = { {} }, presented = presented)
        first.launcher = host.registerForActivityResult(SdkPaymentContract) { first.deliver(it) }
        first.present("md-1")
        host.runs()

        val results = mutableListOf<CheckoutResult>()
        val recreated = PayrdrCheckout(sdk = { SdkPayment.create(config) }, onResult = { { results += it } }, presented = presented)
        recreated.launcher = host.registerForActivityResult(SdkPaymentContract) { recreated.deliver(it) }
        recreated.present("md-2")
        host.deliver(CheckoutResult.Unknown(""))

        host.launches.size shouldBe 1
        results shouldBe listOf(CheckoutResult.Unknown("md-1"))
    }

    @Test fun `a launch that fails leaves the checkout free and no run behind`() {
        val host = FakeHost().apply { launchError = IllegalStateException("not attached") }
        val checkout = PayrdrCheckout.Builder(config) {}.build(host)

        shouldThrow<IllegalStateException> { checkout.present("md-1") }
        host.launchError = null
        checkout.present("md-2")

        host.runs().size shouldBe 1
    }

    @Test fun `a session result without an order id is not given the session id`() {
        val host = FakeHost()
        val results = mutableListOf<CheckoutResult>()
        val checkout = PayrdrCheckout.Builder(config) { results += it }.build(host)

        checkout.presentSession("ps_not_an_order")
        host.runs()
        host.deliver(CheckoutResult.Unknown(""))

        results shouldBe listOf(CheckoutResult.Unknown(""))
    }

    @Test fun `building after the host started explains where to build it`() {
        val error = shouldThrow<IllegalStateException> { PayrdrCheckout.Builder(config) {}.build(FakeHost(Lifecycle.State.STARTED)) }
        error.message shouldContain "in a field or in onCreate"
    }

    @Test fun `a second present while the checkout is open opens nothing`() {
        val host = FakeHost()
        val checkout = PayrdrCheckout.Builder(config) {}.build(host)

        checkout.present("md-1")
        checkout.present("md-2")

        host.runs().size shouldBe 1
    }

    @Test fun `after a result the next present opens again`() {
        val host = FakeHost()
        val checkout = PayrdrCheckout.Builder(config) {}.build(host)

        checkout.present("md-1")
        host.deliver(CheckoutResult.Canceled("md-1"))
        checkout.present("md-2")

        host.runs().size shouldBe 2
    }

    @Test fun `a result with no order id names the order that was presented`() {
        val host = FakeHost()
        val results = mutableListOf<CheckoutResult>()
        val checkout = PayrdrCheckout.Builder(config) { results += it }.build(host)

        checkout.present("md-1")
        host.runs()
        host.deliver(CheckoutResult.Unknown(""))

        results shouldBe listOf(CheckoutResult.Unknown("md-1"))
    }
}
