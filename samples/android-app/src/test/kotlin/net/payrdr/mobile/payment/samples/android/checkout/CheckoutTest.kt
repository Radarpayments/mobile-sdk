package net.payrdr.mobile.payment.samples.android.checkout

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistrar
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistration
import net.payrdr.mobile.payment.samples.android.store.model.Scenario
import net.payrdr.mobile.payment.samples.android.store.model.SheetThemeOverride
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutTest {
    private val coverTheme = PaymentTheme.Light
    private val gpay = GooglePayConfig(merchantId = "m", merchantFullName = "Demo", gateway = "example", gatewayMerchantId = "m")

    private fun config(
        scenario: Scenario = Scenario(),
        override: SheetThemeOverride = SheetThemeOverride.COVER,
    ) = buildSdkConfig("https://gw/payment", scenario, coverTheme, override, locale = null, googlePay = gpay)

    @Test fun `scanner off gives null cardScannerProvider`() {
        assertNull(config(Scenario(scanner = false)).cardScannerProvider)
        assertNotNull(config(Scenario(scanner = true)).cardScannerProvider)
    }

    @Test fun `wallet off gives null googlePay`() {
        assertNull(config(Scenario(wallet = false)).googlePay)
        assertEquals(gpay, config(Scenario(wallet = true)).googlePay)
    }

    @Test fun `sdkResultScreen maps to showResultScreen`() {
        assertTrue(config(Scenario(sdkResultScreen = true)).showResultScreen)
    }

    @Test fun `override COVER keeps cover theme`() {
        assertSame(coverTheme, config(override = SheetThemeOverride.COVER).theme)
    }

    @Test fun `override DARK and SYSTEM give stock themes`() {
        assertEquals(PaymentTheme.Dark, config(override = SheetThemeOverride.DARK).theme)
        assertEquals(PaymentTheme.System, config(override = SheetThemeOverride.SYSTEM).theme)
    }

    @Test fun `completed result maps to Success`() {
        assertEquals(CheckoutOutcome.Success("p-1"), CheckoutResult.Completed("p-1").toOutcome())
    }

    @Test fun `canceled result maps to Cancelled`() {
        assertEquals(CheckoutOutcome.Cancelled, CheckoutResult.Canceled("p-1").toOutcome())
    }

    @Test fun `failed result maps to Declined with the reason`() {
        val result = CheckoutResult.Failed("p-1", SdkException("Declined by issuer"))
        assertEquals(CheckoutOutcome.Declined("Declined by issuer"), result.toOutcome())
    }

    @Test fun `unknown result maps to Unconfirmed and not to a decline`() {
        assertEquals(CheckoutOutcome.Unconfirmed("p-1"), CheckoutResult.Unknown("p-1").toOutcome())
    }

    @Test fun `pay while Registering is ignored`() =
        runTest(UnconfinedTestDispatcher()) {
            val pending = CompletableDeferred<OrderRegistration>()
            var calls = 0
            val launched = mutableListOf<OrderRegistration.Registered>()
            val checkout =
                StoreCheckout(
                    registrar = OrderRegistrar { _, _ -> calls++.let { pending.await() } },
                    scope = this,
                    launch = { launched += it },
                    onMdOrder = {},
                )
            checkout.pay(100, null)
            checkout.pay(100, null)
            assertEquals(CheckoutState.Registering, checkout.state.value)
            pending.complete(OrderRegistration.Registered("md-1"))
            assertEquals(1, calls)
            assertEquals(listOf(OrderRegistration.Registered("md-1")), launched)
            assertEquals(CheckoutState.InSheet, checkout.state.value)
        }

    @Test fun `an API V2 session is launched as a session and reported by its mdOrder`() =
        runTest(UnconfinedTestDispatcher()) {
            val launched = mutableListOf<OrderRegistration.Registered>()
            val reported = mutableListOf<String>()
            val session = OrderRegistration.Registered(mdOrder = "md-1", sessionId = "ps_1")
            val checkout =
                StoreCheckout(
                    registrar = { _, _ -> session },
                    scope = this,
                    launch = { launched += it },
                    onMdOrder = { reported += it },
                )
            checkout.pay(100, null)
            assertEquals(listOf(session), launched)
            assertEquals(listOf("md-1"), reported)
        }

    @Test fun `missing credentials ends in RegistrationFailed without launch`() =
        runTest(UnconfinedTestDispatcher()) {
            val launched = mutableListOf<OrderRegistration.Registered>()
            val checkout =
                StoreCheckout(
                    registrar = { _, _ -> OrderRegistration.MissingCredentials },
                    scope = this,
                    launch = { launched += it },
                    onMdOrder = {},
                )
            checkout.pay(100, null)
            assertEquals(CheckoutState.RegistrationFailed(OrderRegistration.MissingCredentials), checkout.state.value)
            assertTrue(launched.isEmpty())
        }

    @Test fun `result returns to Idle`() =
        runTest(UnconfinedTestDispatcher()) {
            val checkout =
                StoreCheckout(
                    registrar = { _, _ -> OrderRegistration.Registered("md-1") },
                    scope = this,
                    launch = {},
                    onMdOrder = {},
                )
            checkout.pay(100, null)
            checkout.onSheetClosed()
            assertEquals(CheckoutState.Idle, checkout.state.value)
        }
}
