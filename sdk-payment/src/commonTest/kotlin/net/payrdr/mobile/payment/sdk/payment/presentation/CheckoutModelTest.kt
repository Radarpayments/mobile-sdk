package net.payrdr.mobile.payment.sdk.payment.presentation

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.BindingItem
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.FinishedPaymentInfoResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.flow.PaymentFlowController
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.result.SavedCardInfo
import net.payrdr.mobile.payment.sdk.payment.support.FakeApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakeGooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakeKeyProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakePaymentApi
import net.payrdr.mobile.payment.sdk.payment.support.FakeWebChallengeHost
import net.payrdr.mobile.payment.sdk.payment.support.Fixtures
import net.payrdr.mobile.payment.sdk.payment.ui.Route
import net.payrdr.mobile.payment.sdk.payment.ui.Step
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability
import net.payrdr.mobile.payment.sdk.payment.ui.loading.KeyValueStore
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutModelTest {
    private class MemoryStore : KeyValueStore {
        val values = mutableMapOf<String, Int>()

        override fun getInt(key: String): Int? = values[key]

        override fun putInt(
            key: String,
            value: Int,
        ) {
            values[key] = value
        }
    }

    private class Checkout(
        val model: CheckoutModel,
        val controller: PaymentFlowController,
        val api: FakePaymentApi,
        val webHost: FakeWebChallengeHost,
        val store: MemoryStore,
        val results: List<CheckoutResult>,
    ) {
        val ui: CheckoutUi get() = model.ui.value
        val step: Step get() = ui.step
        val route: Route get() = step.shouldBeInstanceOf<Step.Form>().route
    }

    private val wallet = WalletAvailability(googlePay = true, applePay = true)
    private val threeDs = ProcessFormResponse(errorCode = 0, acsUrl = "https://acs", paReq = "PA", termUrl = "https://t")

    private fun TestScope.checkout(
        cards: Int = 0,
        wallets: WalletAvailability? = wallet,
        sheet: Boolean = true,
        showResultScreen: Boolean = true,
        reduceMotion: Boolean = false,
        cvcNotRequired: Boolean = false,
    ): Checkout {
        val api = FakePaymentApi()
        api.nextSessionStatus =
            api.nextSessionStatus.copy(
                cvcNotRequired = cvcNotRequired,
                bindingItems = List(cards) { BindingItem(id = "b-$it", label = "411111**1111 12/26", paymentSystem = "VISA") },
            )
        val webHost = FakeWebChallengeHost()
        val controller =
            PaymentFlowController(
                api = api,
                keyProvider = FakeKeyProvider(),
                core = SdkCore(),
                webChallengeHost = webHost,
                logger = Logger(),
                scope = this,
                googlePayTokenProvider = FakeGooglePayTokenProvider(),
                applePayTokenProvider = FakeApplePayTokenProvider(),
                googlePay = GooglePayConfig(),
                applePay = ApplePayConfig(merchantId = "merchant.net.payrdr.test"),
            )
        val store = MemoryStore()
        val results = mutableListOf<CheckoutResult>()
        val model =
            CheckoutModel(
                config = SdkPaymentConfig(baseUrl = "https://pay.test", showResultScreen = showResultScreen),
                controller = controller,
                store = store,
                wallets = wallets,
                sheetPresentation = sheet,
                reduceMotion = { reduceMotion },
                scope = backgroundScope,
                onResult = { results += it },
            )
        return Checkout(model, controller, api, webHost, store, results)
    }

    private fun TestScope.started(
        cards: Int = 0,
        wallets: WalletAvailability? = wallet,
        sheet: Boolean = true,
        showResultScreen: Boolean = true,
        reduceMotion: Boolean = false,
        cvcNotRequired: Boolean = false,
    ): Checkout {
        val checkout = checkout(cards, wallets, sheet, showResultScreen, reduceMotion, cvcNotRequired)
        checkout.start()
        settle()
        return checkout
    }

    /** The model follows the flow in the background scope, which `advanceUntilIdle` alone does not run to the end. */
    private fun TestScope.settle() {
        advanceUntilIdle()
        runCurrent()
    }

    private fun Checkout.start() {
        controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
    }

    private fun Checkout.submitCard() {
        model.handle(
            PaymentIntent.SubmitNewCard(
                pan = Fixtures.TEST_PAN,
                expiryMMYY = Fixtures.TEST_EXPIRY,
                cvc = Fixtures.TEST_CVC,
                cardHolder = Fixtures.TEST_CARDHOLDER,
                saveCard = false,
            ),
        )
    }

    private fun Checkout.declines() {
        api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(status = "DECLINED", actionCode = 0)
    }

    private fun Checkout.card(index: Int = 0): SavedCardInfo = step.shouldBeInstanceOf<Step.Form>().form.savedCards[index]

    @Test fun `starts on a blank loading step`() =
        runTest {
            val checkout = checkout()
            runCurrent()

            checkout.step shouldBe Step.Loading
            checkout.ui.skeletonVisible shouldBe false
            checkout.ui.shown shouldBe true
            checkout.ui.forward shouldBe true
        }

    @Test fun `session in before the skeleton delay - the form comes without a skeleton`() =
        runTest {
            val checkout = checkout()
            advanceTimeBy(299)
            checkout.start()
            runCurrent()

            checkout.route shouldBe Route.Methods
            checkout.ui.skeletonVisible shouldBe false
            checkout.ui.zoomReveal shouldBe true
            checkout.ui.revealKey shouldBe "methods"
        }

    @Test fun `session later than the delay - the skeleton stays for its minimum time`() =
        runTest {
            val checkout = checkout()
            advanceTimeBy(300)
            runCurrent()
            checkout.ui.skeletonVisible shouldBe true

            checkout.start()
            runCurrent()
            advanceTimeBy(399)
            runCurrent()
            checkout.step shouldBe Step.Loading
            checkout.ui.skeletonVisible shouldBe true

            advanceTimeBy(1)
            runCurrent()
            checkout.route shouldBe Route.Methods
            checkout.ui.skeletonVisible shouldBe false
            checkout.ui.zoomReveal shouldBe false
            checkout.ui.revealKey shouldBe "methods"
        }

    @Test fun `skeleton rows come from the last session on the device`() =
        runTest {
            val first = started(cards = 2)
            first.store.values["payrdr.savedCardCount.https://pay.test"] shouldBe 2
            first.store.values["payrdr.walletOffered.https://pay.test"] shouldBe 1
            first.ui.skeleton shouldBe SkeletonLayout(wallet = false, savedCards = 0)
        }

    @Test fun `no cards and no wallet - the card form is the root`() =
        runTest {
            val checkout = started(wallets = WalletAvailability.None)

            checkout.route shouldBe Route.NewCard
            checkout.step.shouldBeInstanceOf<Step.Form>().isRoot shouldBe true
            checkout.ui.wallets shouldBe WalletAvailability.None
        }

    @Test fun `saved cards - the methods are the root`() =
        runTest {
            val checkout = started(cards = 1, wallets = WalletAvailability.None)

            checkout.route shouldBe Route.Methods
        }

    @Test fun `wallet on the device - the methods are the root and offer it`() =
        runTest {
            val checkout = started()

            checkout.route shouldBe Route.Methods
            checkout.ui.wallets shouldBe wallet
        }

    @Test fun `wallets not known yet - loading until they are`() =
        runTest {
            val checkout = started(wallets = null)
            checkout.step shouldBe Step.Loading

            checkout.model.walletsKnown(WalletAvailability.None)
            settle()

            checkout.route shouldBe Route.NewCard
        }

    @Test fun `push goes forward and pop comes back`() =
        runTest {
            val checkout = started()

            checkout.model.push(Route.NewCard)
            checkout.route shouldBe Route.NewCard
            checkout.step.shouldBeInstanceOf<Step.Form>().isRoot shouldBe false
            checkout.ui.forward shouldBe true
            checkout.ui.revealKey shouldBe null

            checkout.model.pop()
            checkout.route shouldBe Route.Methods
            checkout.ui.forward shouldBe false
            checkout.results shouldBe emptyList()
        }

    @Test fun `pop from the root cancels`() =
        runTest {
            val checkout = started()

            checkout.model.pop()
            settle()

            checkout.results shouldBe emptyList()
            checkout.ui.shown shouldBe false
            checkout.model.exitFinished()
            checkout.results shouldBe listOf(CheckoutResult.Canceled(Fixtures.TEST_MD_ORDER))
        }

    @Test fun `back on a form pops`() =
        runTest {
            val checkout = started()
            checkout.model.push(Route.AllCards)

            checkout.model.back()

            checkout.route shouldBe Route.Methods
        }

    @Test fun `back while loading cancels`() =
        runTest {
            val checkout = checkout(sheet = false)
            runCurrent()

            checkout.model.back()
            settle()

            checkout.results.single().shouldBeInstanceOf<CheckoutResult.Canceled>()
        }

    @Test fun `back does nothing while the payment is processed`() =
        runTest {
            val checkout = started()
            checkout.api.processFormGate = CompletableDeferred()
            checkout.submitCard()
            runCurrent()
            checkout.step.shouldBeInstanceOf<Step.Processing>()

            checkout.model.back()
            runCurrent()

            checkout.step.shouldBeInstanceOf<Step.Processing>()
            checkout.results shouldBe emptyList()
            checkout.api.processFormGate?.complete(Unit)
            settle()
        }

    @Test fun `back on the result hands it over`() =
        runTest {
            val checkout = started(sheet = false)
            checkout.submitCard()
            settle()
            checkout.step.shouldBeInstanceOf<Step.Result>()
            checkout.results shouldBe emptyList()

            checkout.model.back()

            checkout.results shouldBe listOf(CheckoutResult.Completed(Fixtures.TEST_MD_ORDER))
            checkout.step.shouldBeInstanceOf<Step.Result>()
        }

    @Test fun `scrim tap cancels on the sheet steps only`() =
        runTest {
            val checkout = started(sheet = false)
            checkout.model.push(Route.NewCard)

            checkout.model.scrimTapped()
            settle()
            checkout.results shouldBe emptyList()

            checkout.model.pop()
            checkout.model.scrimTapped()
            settle()
            checkout.results.single().shouldBeInstanceOf<CheckoutResult.Canceled>()
        }

    @Test fun `sheet dismissed on a pushed step pops and on the methods cancels`() =
        runTest {
            val checkout = started()
            checkout.model.push(Route.NewCard)

            checkout.model.sheetDismissed()
            checkout.route shouldBe Route.Methods
            checkout.ui.shown shouldBe true

            checkout.model.sheetDismissed()
            settle()
            checkout.ui.shown shouldBe false
        }

    @Test fun `sheet dismissed on the result hands it over`() =
        runTest {
            val checkout = started()
            checkout.submitCard()
            settle()

            checkout.model.sheetDismissed()
            checkout.model.exitFinished()

            checkout.results shouldBe listOf(CheckoutResult.Completed(Fixtures.TEST_MD_ORDER))
        }

    @Test fun `saved card that needs its CVC opens its own step`() =
        runTest {
            val checkout = started(cards = 1)
            val card = checkout.card()

            checkout.model.opensCardStep(card) shouldBe true
            checkout.model.paySaved(card)

            checkout.route shouldBe Route.Saved(card)
            checkout.api.processBindingFormCalls.size shouldBe 0
        }

    @Test fun `saved card that needs nothing pays at once`() =
        runTest {
            val checkout = started(cards = 1, cvcNotRequired = true)
            val card = checkout.card()

            checkout.model.opensCardStep(card) shouldBe false
            checkout.model.paySaved(card)
            settle()

            checkout.api.processBindingFormCalls.size shouldBe 1
            checkout.step.shouldBeInstanceOf<Step.Result>()
        }

    @Test fun `result is held for the result screen and delivered on close`() =
        runTest {
            val checkout = started(sheet = false)
            checkout.submitCard()
            settle()

            checkout.step
                .shouldBeInstanceOf<Step.Result>()
                .completed.result shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
            checkout.ui.forward shouldBe true
            checkout.results shouldBe emptyList()

            checkout.model.closeResult()
            checkout.model.closeResult()

            checkout.results shouldBe listOf(CheckoutResult.Completed(Fixtures.TEST_MD_ORDER))
        }

    @Test fun `failed result is held too`() =
        runTest {
            val checkout = started(sheet = false)
            checkout.declines()
            checkout.submitCard()
            settle()

            checkout.step
                .shouldBeInstanceOf<Step.Result>()
                .completed.result
                .shouldBeInstanceOf<CheckoutResult.Failed>()
            checkout.ui.canReturnToForm shouldBe true
            checkout.results shouldBe emptyList()
        }

    @Test fun `canceled result is never held`() =
        runTest {
            val checkout = started(sheet = false)

            checkout.model.handle(PaymentIntent.Cancel)
            settle()

            checkout.results shouldBe listOf(CheckoutResult.Canceled(Fixtures.TEST_MD_ORDER))
            checkout.route shouldBe Route.Methods
        }

    @Test fun `without the result screen the result is delivered at once and the last step stays`() =
        runTest {
            val checkout = started(sheet = false, showResultScreen = false)
            checkout.submitCard()
            settle()

            checkout.results shouldBe listOf(CheckoutResult.Completed(Fixtures.TEST_MD_ORDER))
            (checkout.step is Step.Result) shouldBe false
        }

    @Test fun `sheet slides away before the result is delivered and only once`() =
        runTest {
            val checkout = started(showResultScreen = false)
            checkout.submitCard()
            settle()

            checkout.ui.shown shouldBe false
            checkout.results shouldBe emptyList()

            checkout.model.exitFinished()
            checkout.model.exitFinished()

            checkout.results shouldBe listOf(CheckoutResult.Completed(Fixtures.TEST_MD_ORDER))
        }

    @Test fun `exit finished before anything is delivered does nothing`() =
        runTest {
            val checkout = started()

            checkout.model.exitFinished()

            checkout.results shouldBe emptyList()
            checkout.ui.shown shouldBe true
        }

    @Test fun `retry from a failed result returns to the root of the form`() =
        runTest {
            val checkout = started(sheet = false)
            checkout.model.push(Route.NewCard)
            checkout.declines()
            checkout.submitCard()
            settle()
            checkout.step.shouldBeInstanceOf<Step.Result>()

            checkout.model.retryFromResult()
            settle()

            checkout.route shouldBe Route.Methods
            checkout.step.shouldBeInstanceOf<Step.Form>().isRoot shouldBe true
            checkout.ui.forward shouldBe false
            checkout.results shouldBe emptyList()
        }

    @Test fun `bank page waits for the sheet to expand`() =
        runTest {
            val checkout = started()
            checkout.api.nextProcessFormResponse = threeDs
            checkout.submitCard()
            settle()

            checkout.step shouldBe Step.ThreeDs
            checkout.webHost.performedChallenges.size shouldBe 0

            checkout.model.sheetExpanded()
            settle()

            checkout.webHost.performedChallenges.size shouldBe 1
            checkout.step.shouldBeInstanceOf<Step.Result>()
        }

    @Test fun `bank page opens at once on a page`() =
        runTest {
            val checkout = started(sheet = false)
            checkout.api.nextProcessFormResponse = threeDs
            checkout.submitCard()
            settle()

            checkout.webHost.performedChallenges.size shouldBe 1
        }

    @Test fun `bank page opens at once with reduced motion`() =
        runTest {
            val checkout = started(reduceMotion = true)
            checkout.api.nextProcessFormResponse = threeDs
            checkout.submitCard()
            settle()

            checkout.webHost.performedChallenges.size shouldBe 1
        }

    @Test fun `dispose lets a waiting bank page go on`() =
        runTest {
            val checkout = started()
            checkout.api.nextProcessFormResponse = threeDs
            checkout.submitCard()
            settle()
            checkout.webHost.performedChallenges.size shouldBe 0

            checkout.model.dispose()
            settle()

            checkout.webHost.performedChallenges.size shouldBe 1
        }

    @Test fun `order summary and the flags of the form reach the renderer`() =
        runTest {
            val checkout = started()

            checkout.ui.order shouldBe checkout.controller.order.value
            checkout.ui.transfer shouldBe false
            checkout.ui.saveCardOnly shouldBe false
            checkout.ui.sheetPresentation shouldBe true
            checkout.ui.walletConfigured shouldBe false
            checkout.controller.state.value
                .shouldBeInstanceOf<PaymentState.CardFormShown>()
        }
}
