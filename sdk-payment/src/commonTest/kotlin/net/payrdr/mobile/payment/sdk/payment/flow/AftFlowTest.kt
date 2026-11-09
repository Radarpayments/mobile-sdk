package net.payrdr.mobile.payment.sdk.payment.flow

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.core.validation.CardRules
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.P2PFromCard
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.WalletVendor
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.BindingItem
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.FinishedPaymentInfoResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.PayerParam
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNotConfiguredException
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.support.FakeApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakeGooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakeKeyProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakePaymentApi
import net.payrdr.mobile.payment.sdk.payment.support.FakeWebChallengeHost
import net.payrdr.mobile.payment.sdk.payment.support.Fixtures
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test

/** One-leg AFT transfers: P2P orders with `WITHOUT_TO_CARD`, paid through `p2p/perform.do`. */
@OptIn(ExperimentalCoroutinesApi::class)
class AftFlowTest {
    private val debit = BindingItem(id = "b-debit", label = "417666**0104 02/28", paymentSystem = "VISA", bindingCategory = "D")
    private val common = BindingItem(id = "b-common", label = "411111**1111 12/26", paymentSystem = "VISA", bindingCategory = "C")

    private fun aft(
        features: List<String> = listOf("WITHOUT_TO_CARD"),
        options: List<String> = listOf("CARD", "VISA", "GOOGLEPAY", "APPLEPAY"),
        cvcBindingNotRequired: Boolean = false,
    ): SessionStatusResponse =
        FakePaymentApi().nextSessionStatus.copy(
            orderPaymentWay = "P2P",
            orderFeatures = features,
            merchantOptions = options,
            bindingEnabled = true,
            bindingItems = listOf(debit, common),
            cvcNotRequired = true,
            cvcBindingNotRequired = cvcBindingNotRequired,
        )

    private class Built(
        val api: FakePaymentApi,
        val webHost: FakeWebChallengeHost,
        val keyProvider: FakeKeyProvider,
        val googlePay: FakeGooglePayTokenProvider,
        val applePay: FakeApplePayTokenProvider,
        val controller: PaymentFlowController,
    )

    private fun TestScope.start(session: SessionStatusResponse): Built {
        val api = FakePaymentApi().apply { nextSessionStatus = session }
        val webHost = FakeWebChallengeHost()
        val keyProvider = FakeKeyProvider()
        val googlePay = FakeGooglePayTokenProvider()
        val applePay = FakeApplePayTokenProvider()
        val controller =
            PaymentFlowController(
                api = api,
                keyProvider = keyProvider,
                core = SdkCore(),
                webChallengeHost = webHost,
                logger = Logger(),
                scope = this,
                googlePayTokenProvider = googlePay,
                applePayTokenProvider = applePay,
                googlePay = GooglePayConfig(),
                applePay = ApplePayConfig(merchantId = "merchant.net.payrdr.test"),
            )
        controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
        advanceUntilIdle()
        return Built(api, webHost, keyProvider, googlePay, applePay, controller)
    }

    private fun Built.form() = controller.state.value.shouldBeInstanceOf<PaymentState.CardFormShown>()

    private fun Built.terminal() =
        controller.state.value
            .shouldBeInstanceOf<PaymentState.Completed>()
            .result

    private fun newCard(saveCard: Boolean = false) =
        PaymentIntent.SubmitNewCard(
            pan = Fixtures.TEST_PAN,
            expiryMMYY = Fixtures.TEST_EXPIRY,
            cvc = Fixtures.TEST_CVC,
            cardHolder = Fixtures.TEST_CARDHOLDER,
            saveCard = saveCard,
            payerData = mapOf(PayerField.PAYER_FIRST_NAME to "Test", PayerField.RECIPIENT_ACCOUNT to "acc-1"),
        )

    @Test fun `an AFT order is a transfer with only debit cards and the wallets the gateway enabled`() =
        runTest {
            val form = start(aft()).form()

            form.transfer shouldBe true
            form.savedCards.map { it.bindingId } shouldBe listOf("b-debit")
            form.googlePay.shouldNotBeNull()
            form.applePay.shouldNotBeNull()
        }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `Google Pay pays a transfer through p2p perform with its token in base64 and saves no card`() =
        runTest {
            // Saving is offered for a card: a wallet still saves nothing.
            val built = start(aft(options = listOf("CARD", "GOOGLEPAY", "DEBIT_BINDING_ALLOWED")))
            built.controller.handle(PaymentIntent.TapGooglePay)
            advanceUntilIdle()

            built.api.googlePayCalls.shouldBeEmpty()
            val request = built.api.p2pPerformCalls.single()
            request.mdOrder shouldBe Fixtures.TEST_MD_ORDER
            // The gateway reads the token as base64, as its own payment page sends it.
            request.fromCard shouldBe P2PFromCard.Wallet(WalletVendor.GOOGLE, Base64.encode("FAKE_JWE_TOKEN".encodeToByteArray()))
            request.bindingNotNeeded shouldBe true
            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `Apple Pay pays a transfer through p2p perform with its token and saves no card`() =
        runTest {
            val built = start(aft(options = listOf("CARD", "APPLEPAY", "DEBIT_BINDING_ALLOWED")))
            built.controller.handle(PaymentIntent.TapApplePay)
            advanceUntilIdle()

            built.api.applePayCalls.shouldBeEmpty()
            val request = built.api.p2pPerformCalls.single()
            request.fromCard shouldBe P2PFromCard.Wallet(WalletVendor.APPLE, "FAKE_APPLE_PAY_TOKEN_B64")
            request.bindingNotNeeded shouldBe true
            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `a wallet transfer that needs 3-D Secure opens the bank page and ends with the order status`() =
        runTest {
            val built = start(aft())
            built.api.nextP2PPerformResponse =
                ProcessFormResponse(errorCode = 0, acsUrl = "https://gw/getwhitepageurl.do", paReq = "paReq", termUrl = "termUrl")
            built.api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(status = "DEPOSITED")
            built.controller.handle(PaymentIntent.TapGooglePay)
            advanceUntilIdle()

            built.webHost.performedChallenges
                .single()
                .acsUrl shouldBe "https://gw/getwhitepageurl.do"
            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `a refused wallet transfer fails with the gateway's message`() =
        runTest {
            val built = start(aft())
            built.api.nextP2PPerformResponse = ProcessFormResponse(errorCode = 5, error = "Access denied")
            built.controller.handle(PaymentIntent.TapApplePay)
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error.message shouldBe "Access denied"
        }

    @Test fun `a transfer refused by its code alone fails at once`() =
        runTest {
            val built = start(aft())
            built.api.nextP2PPerformResponse = ProcessFormResponse(errorCode = 5)
            built.controller.handle(PaymentIntent.TapApplePay)
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkDeclinedException>()
            built.api.getFinishedPaymentInfoCalls.shouldBeEmpty()
        }

    @Test fun `no wallets for a transfer that must save the card - a wallet saves none`() =
        runTest {
            val wallets = listOf("CARD", "GOOGLEPAY", "APPLEPAY", "DEBIT_BINDING_ALLOWED")
            for (features in listOf(
                listOf("WITHOUT_TO_CARD", "FORCE_CREATE_BINDING"),
                listOf("WITHOUT_TO_CARD", "VERIFY", "FORCE_CREATE_BINDING"),
            )) {
                val form = start(aft(features = features, options = wallets)).form()
                form.googlePay.shouldBeNull()
                form.applePay.shouldBeNull()
            }
        }

    @Test fun `no wallets for a transfer that asks the buyer for mandatory details - a wallet has no form to send them`() =
        runTest {
            val asked = { mandatory: Boolean ->
                aft().copy(payerDataParamsNeedToBeFilled = mapOf("VISA" to listOf(PayerParam("RECIPIENT_ACCOUNT", mandatory = mandatory))))
            }
            val required = start(asked(true)).form()
            required.googlePay.shouldBeNull()
            required.applePay.shouldBeNull()

            val optional = start(asked(false)).form()
            optional.googlePay.shouldNotBeNull()
            optional.applePay.shouldNotBeNull()
        }

    @Test fun `a saved card asks for its CVC unless the gateway waives it for transfers`() =
        runTest {
            // cvcNotRequired is the e-commerce flag; a transfer follows cvcBindingNotRequired.
            start(aft(cvcBindingNotRequired = false)).form().cvcRequired shouldBe true
            start(aft(cvcBindingNotRequired = true)).form().cvcRequired shouldBe false
        }

    @Test fun `saving the card is offered only to a merchant allowed debit bindings`() =
        runTest {
            start(aft()).form().canSaveCard shouldBe false
            val allowed = start(aft(options = listOf("CARD", "VISA", "DEBIT_BINDING_ALLOWED"))).form()
            allowed.canSaveCard shouldBe true
            allowed.saveCardLocked shouldBe false
            val forced = start(aft(features = listOf("WITHOUT_TO_CARD", "FORCE_CREATE_BINDING"), options = listOf("DEBIT_BINDING_ALLOWED")))
            forced.form().canSaveCard shouldBe true
            forced.form().saveCardLocked shouldBe true
        }

    @Test fun `BINDING_NOT_NEEDED forbids saving a new card but keeps the saved debit cards`() =
        runTest {
            val refused = start(aft(features = listOf("WITHOUT_TO_CARD", "BINDING_NOT_NEEDED"), options = listOf("DEBIT_BINDING_ALLOWED")))
            refused.form().canSaveCard shouldBe false
            refused.form().savedCards.map { it.bindingId } shouldBe listOf("b-debit")
            refused.controller.handle(newCard(saveCard = true))
            advanceUntilIdle()
            refused.api.p2pPerformCalls
                .single()
                .bindingNotNeeded shouldBe true
        }

    @Test fun `no saved cards and no saving without bindingEnabled`() =
        runTest {
            val form = start(aft(options = listOf("DEBIT_BINDING_ALLOWED")).copy(bindingEnabled = false)).form()
            form.savedCards.shouldBeEmpty()
            form.canSaveCard shouldBe false
        }

    @Test fun `FORCE_CREATE_BINDING always asks the gateway to save the card even with no switch to show`() =
        runTest {
            // Without DEBIT_BINDING_ALLOWED the switch is hidden, but sending true would cancel what the merchant asked for.
            val built = start(aft(features = listOf("WITHOUT_TO_CARD", "FORCE_CREATE_BINDING")))
            built.form().canSaveCard shouldBe false
            built.controller.handle(newCard(saveCard = false))
            advanceUntilIdle()
            built.api.p2pPerformCalls
                .single()
                .bindingNotNeeded shouldBe false
        }

    @Test fun `a new card goes to p2p perform with the payer and recipient fields`() =
        runTest {
            val built = start(aft())
            built.controller.handle(newCard(saveCard = true))
            advanceUntilIdle()

            built.api.processFormCalls.shouldBeEmpty()
            val request = built.api.p2pPerformCalls.single()
            request.mdOrder shouldBe Fixtures.TEST_MD_ORDER
            request.fromCard.shouldBeInstanceOf<P2PFromCard.SeToken>()
            // Saving is not offered to this merchant, so the switch value does not count.
            request.bindingNotNeeded shouldBe true
            request.payerData shouldBe mapOf(PayerField.PAYER_FIRST_NAME to "Test", PayerField.RECIPIENT_ACCOUNT to "acc-1")
            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `bindingNotNeeded follows the save switch when it is offered and is false when it is locked`() =
        runTest {
            val offered = start(aft(options = listOf("DEBIT_BINDING_ALLOWED")))
            offered.controller.handle(newCard(saveCard = true))
            advanceUntilIdle()
            offered.api.p2pPerformCalls
                .single()
                .bindingNotNeeded shouldBe false

            val locked = start(aft(features = listOf("WITHOUT_TO_CARD", "FORCE_CREATE_BINDING"), options = listOf("DEBIT_BINDING_ALLOWED")))
            locked.controller.handle(newCard(saveCard = false))
            advanceUntilIdle()
            locked.api.p2pPerformCalls
                .single()
                .bindingNotNeeded shouldBe false
        }

    @Test fun `a debit card without CVC goes to performByBinding by its id`() =
        runTest {
            val built = start(aft(cvcBindingNotRequired = true))
            built.controller.handle(PaymentIntent.SubmitSavedCard(bindingId = "b-debit", cvc = null))
            advanceUntilIdle()

            built.api.processBindingFormCalls.shouldBeEmpty()
            val request = built.api.p2pPerformByBindingCalls.single()
            request.fromCard shouldBe P2PFromCard.Binding("b-debit")
            request.bindingNotNeeded shouldBe true
            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `a debit card with CVC goes to performByBinding as a seToken`() =
        runTest {
            val built = start(aft(cvcBindingNotRequired = false))
            built.controller.handle(PaymentIntent.SubmitSavedCard(bindingId = "b-debit", cvc = "123"))
            advanceUntilIdle()

            built.api.p2pPerformByBindingCalls
                .single()
                .fromCard
                .shouldBeInstanceOf<P2PFromCard.SeToken>()
        }

    @Test fun `a transfer that needs 3-D Secure opens the bank page and ends with the order status`() =
        runTest {
            val built = start(aft())
            built.api.nextP2PPerformResponse =
                ProcessFormResponse(
                    errorCode = 0,
                    acsUrl = "https://gw/getwhitepageurl.do",
                    paReq = "White page paReq",
                    termUrl = "White page termUrl",
                )
            built.api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(status = "DEPOSITED")
            built.controller.handle(newCard())
            advanceUntilIdle()

            built.webHost.performedChallenges
                .single()
                .acsUrl shouldBe "https://gw/getwhitepageurl.do"
            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `a gateway error of the transfer fails with its message`() =
        runTest {
            val built = start(aft())
            built.api.nextP2PPerformResponse = ProcessFormResponse(errorCode = 7, error = "System error")
            built.controller.handle(newCard())
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error.message shouldBe "System error"
        }

    @Test fun `P2P orders other than a one-leg AFT are not supported`() =
        runTest {
            val features = listOf(listOf("WITHOUT_FROM_CARD"), emptyList(), listOf("WITHOUT_FROM_CARD", "VERIFY", "FORCE_CREATE_BINDING"))
            for (features in features) {
                val built = start(aft(features = features))
                built
                    .terminal()
                    .shouldBeInstanceOf<CheckoutResult.Failed>()
                    .error
                    .shouldBeInstanceOf<SdkNotConfiguredException>()
            }
        }

    private fun verify(options: List<String> = listOf("CARD", "VISA", "DEBIT_BINDING_ALLOWED")) =
        aft(features = listOf("WITHOUT_TO_CARD", "VERIFY", "FORCE_CREATE_BINDING"), options = options).copy(amount = "0")

    @Test fun `a card-saving VERIFY order takes a new card only with no switch and no amount`() =
        runTest {
            val built = start(verify())
            val form = built.form()

            form.transfer shouldBe true
            form.saveCardOnly shouldBe true
            form.savedCards.shouldBeEmpty()
            form.canSaveCard shouldBe false
            // Nothing is paid: no amount on the buttons, the processing step or the result.
            built.controller.order.value
                ?.formattedAmount
                .shouldBeNull()
        }

    @Test fun `a card-saving order sends the new card to p2p perform and asks to save it`() =
        runTest {
            val built = start(verify())
            built.api.nextFinishedPaymentInfo =
                FinishedPaymentInfoResponse(status = "REVERSED", actionCode = 0, orderFeatures = listOf("VERIFY"))
            built.controller.handle(newCard(saveCard = false))
            advanceUntilIdle()

            val request = built.api.p2pPerformCalls.single()
            request.fromCard.shouldBeInstanceOf<P2PFromCard.SeToken>()
            // true would be refused before 3-D Secure: a VERIFY order exists to save the card.
            request.bindingNotNeeded shouldBe false
            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `a card-saving order ends saved when the zero payment is reversed with action code 0`() =
        runTest {
            val saved = start(verify())
            saved.api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(status = "REVERSED", actionCode = 0)
            saved.controller.handle(newCard())
            advanceUntilIdle()
            saved.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
            // Nothing was paid, so the receipt carries no amount.
            (saved.controller.state.value as PaymentState.Completed)
                .receipt
                ?.formattedAmount
                .shouldBeNull()

            val declined = start(verify())
            declined.api.nextFinishedPaymentInfo =
                FinishedPaymentInfoResponse(status = "DECLINED", actionCode = 2001, actionCodeDescription = "Card is blocked")
            declined.controller.handle(newCard())
            advanceUntilIdle()
            declined.terminal().shouldBeInstanceOf<CheckoutResult.Failed>()
        }

    @Test fun `a reversed transfer is still a failure outside a card-saving order`() =
        runTest {
            val built = start(aft())
            built.api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(status = "REVERSED", actionCode = 0)
            built.controller.handle(newCard())
            advanceUntilIdle()
            built.terminal().shouldBeInstanceOf<CheckoutResult.Failed>()
        }

    @Test fun `a card-saving order opened again after it was reversed with action code 0 is saved`() =
        runTest {
            val api = FakePaymentApi()
            api.nextSessionStatus = verify().copy(remainingSecs = null)
            api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(status = "REVERSED", actionCode = 0, orderFeatures = listOf("VERIFY"))
            val controller =
                PaymentFlowController(
                    api = api,
                    keyProvider = FakeKeyProvider(),
                    core = SdkCore(),
                    webChallengeHost = FakeWebChallengeHost(),
                    logger = Logger(),
                    scope = this,
                )
            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.state.value
                .shouldBeInstanceOf<PaymentState.Completed>()
                .result shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `an e-commerce order keeps processform and its own CVC flag`() =
        runTest {
            val built =
                start(
                    aft().copy(orderPaymentWay = null, orderFeatures = emptyList(), cvcNotRequired = false, cvcBindingNotRequired = true),
                )
            built.form().transfer shouldBe false
            built.form().cvcRequired shouldBe true
            built.form().savedCards.size shouldBe 2
            built.controller.handle(newCard())
            advanceUntilIdle()
            built.api.p2pPerformCalls.shouldBeEmpty()
            built.api.processFormCalls.size shouldBe 1
        }

    @Test fun `a transfer seals a cardholder name into the seToken - the P2P gateway refuses an empty one`() {
        fun holder(
            typed: String?,
            payer: Map<PayerField, String> = emptyMap(),
        ) = transferCardholder(typed, payer)

        holder("JANE DOE") shouldBe "JANE DOE"
        // The form hides the holder by default: the sender's name stands in, as on the gateway's own page.
        holder(null, mapOf(PayerField.PAYER_FIRST_NAME to "Test", PayerField.PAYER_LAST_NAME to "User")) shouldBe "Test User"
        holder(" ", mapOf(PayerField.PAYER_LAST_NAME to "User")) shouldBe "User"
        holder(null) shouldBe "CARDHOLDER"
        // Sender names allow more than a card does: what the card's rules refuse is dropped, not sent to fail tokenization.
        holder(null, mapOf(PayerField.PAYER_FIRST_NAME to "Mary-Jane", PayerField.PAYER_LAST_NAME to "O'Neil")) shouldBe "Mary-Jane O'Neil"
        holder(null, mapOf(PayerField.PAYER_FIRST_NAME to "Anna2", PayerField.PAYER_LAST_NAME to "Smith (Jr)")) shouldBe "Anna Smith Jr"
        holder(null, mapOf(PayerField.PAYER_FIRST_NAME to "Мария")) shouldBe "CARDHOLDER"
        holder(null, mapOf(PayerField.PAYER_FIRST_NAME to "Alexandrina", PayerField.PAYER_LAST_NAME to "Montgomery-Fitzwilliam")) shouldBe
            "Alexandrina Montgomery-Fitzwil"
    }

    @Test fun `every transfer cardholder passes the card's holder rules`() {
        val names = listOf("Mary-Jane", "O'Neil", "Anna2", "Smith (Jr)", "Мария", "Jean-Luc Picard de la Montagne Rouge", "  ", "#1")
        for (first in names) {
            for (last in names) {
                val holder = transferCardholder(null, mapOf(PayerField.PAYER_FIRST_NAME to first, PayerField.PAYER_LAST_NAME to last))
                CardRules.isValidHolder(holder) shouldBe true
            }
        }
    }
}
