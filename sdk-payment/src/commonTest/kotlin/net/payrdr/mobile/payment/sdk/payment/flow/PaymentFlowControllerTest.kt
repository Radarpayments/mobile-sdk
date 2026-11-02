package net.payrdr.mobile.payment.sdk.payment.flow

import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.core.SdkCore
import net.payrdr.mobile.payment.sdk.core.model.Key
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ApplePayProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.BindingItem
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.CustomerDetails
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.FinishedPaymentInfoResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.PayerParam
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormGPayResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.ProcessFormResponse
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.WalletError
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayTokenResult
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkGooglePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNetworkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNotConfiguredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkOrderNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsChallengeFailedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsTimeoutException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkTransactionException
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayTokenResult
import net.payrdr.mobile.payment.sdk.payment.keyprovider.KeyProvider
import net.payrdr.mobile.payment.sdk.payment.logger.LogEvent
import net.payrdr.mobile.payment.sdk.payment.logger.LogTransport
import net.payrdr.mobile.payment.sdk.payment.logger.Logger
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.result.PayerFieldSpec
import net.payrdr.mobile.payment.sdk.payment.result.PaymentIntent
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.support.FakeApplePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakeGooglePayTokenProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakeKeyProvider
import net.payrdr.mobile.payment.sdk.payment.support.FakePaymentApi
import net.payrdr.mobile.payment.sdk.payment.support.FakeWebChallengeHost
import net.payrdr.mobile.payment.sdk.payment.support.Fixtures
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsChallenge
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsResult
import net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentFlowControllerTest {
    private fun TestScope.buildController(
        api: FakePaymentApi = FakePaymentApi(),
        webHost: FakeWebChallengeHost = FakeWebChallengeHost(),
        keyProvider: FakeKeyProvider = FakeKeyProvider(),
        googlePay: FakeGooglePayTokenProvider = FakeGooglePayTokenProvider(),
        applePay: FakeApplePayTokenProvider = FakeApplePayTokenProvider(),
        googlePayConfig: GooglePayConfig? = GooglePayConfig(),
        applePayConfig: ApplePayConfig? = ApplePayConfig(merchantId = "merchant.net.payrdr.test"),
    ): BuildResult {
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
                googlePay = googlePayConfig,
                applePay = applePayConfig,
            )
        return BuildResult(controller, api, webHost, googlePay, applePay)
    }

    private data class BuildResult(
        val controller: PaymentFlowController,
        val api: FakePaymentApi,
        val webHost: FakeWebChallengeHost,
        val googlePay: FakeGooglePayTokenProvider,
        val applePay: FakeApplePayTokenProvider,
    )

    @Test fun `starts from Initializing then transitions to CardFormShown`() =
        runTest {
            val result = buildController()
            val controller = result.controller

            controller.state.test {
                awaitItem() shouldBe PaymentState.Initializing
                controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
                // Transitions: Processing(LOADING_SESSION) → CardFormShown
                awaitItem().shouldBeInstanceOf<PaymentState.Processing>()
                awaitItem().shouldBeInstanceOf<PaymentState.CardFormShown>()
            }
        }

    @Test fun `saved card label is split into masked pan and expiry`() =
        runTest {
            val api = FakePaymentApi()
            api.nextSessionStatus =
                api.nextSessionStatus.copy(
                    bindingItems = listOf(BindingItem(id = "b-1", label = "411111**1111 12/26", paymentSystem = "VISA")),
                )
            val controller = buildController(api = api).controller

            controller.state.test {
                awaitItem() shouldBe PaymentState.Initializing
                controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
                awaitItem().shouldBeInstanceOf<PaymentState.Processing>()
                val card = awaitItem().shouldBeInstanceOf<PaymentState.CardFormShown>().savedCards.single()
                card.maskedPan shouldBe "411111**1111"
                card.expiry shouldBe "12/26"
            }
        }

    @Test fun `payer fields from the session reach the form and the payment request`() =
        runTest {
            val api = FakePaymentApi()
            api.nextSessionStatus =
                api.nextSessionStatus.copy(
                    payerDataParamsNeedToBeFilled = mapOf("VISA" to listOf(PayerParam("EMAIL", mandatory = true))),
                    customerDetails = CustomerDetails(email = "john@example.com"),
                    bindingItems =
                        listOf(BindingItem(id = "b-1", label = "411111**1111 12/26", paymentSystem = "VISA", payerPhone = "+447700900123")),
                )
            val controller = buildController(api = api).controller

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            val form = controller.state.value.shouldBeInstanceOf<PaymentState.CardFormShown>()
            form.payerData.fieldsFor("VISA") shouldBe listOf(PayerFieldSpec(PayerField.EMAIL, mandatory = true))
            form.payerData.prefill shouldBe mapOf(PayerField.EMAIL to "john@example.com")
            form.savedCards.single().payerPrefill shouldBe mapOf(PayerField.MOBILE_PHONE to "+447700900123")

            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                    payerData = mapOf(PayerField.EMAIL to "jane@example.com"),
                ),
            )
            advanceUntilIdle()
            api.processFormCalls.single().payerData shouldBe mapOf(PayerField.EMAIL to "jane@example.com")
        }

    @Test fun `closed session of a paid order completes and other statuses fail with the reason`() =
        runTest {
            suspend fun outcome(
                status: String?,
                checkout: CheckoutConfig = CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER),
            ): CheckoutResult {
                val api = FakePaymentApi()
                api.nextSessionStatus = api.nextSessionStatus.copy(remainingSecs = null)
                api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(status = status)
                val controller = buildController(api = api).controller
                controller.handle(PaymentIntent.Start(checkout))
                advanceUntilIdle()
                val completed = controller.state.value.shouldBeInstanceOf<PaymentState.Completed>()
                api.processFormCalls.shouldBeEmpty()
                return completed.result
            }

            outcome("DEPOSITED") shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
            outcome("DECLINED").shouldBeInstanceOf<CheckoutResult.Failed>().error.shouldBeInstanceOf<SdkDeclinedException>()
            outcome(null).shouldBeInstanceOf<CheckoutResult.Failed>().error.shouldBeInstanceOf<SdkOrderNotFoundException>()
            outcome("CREATED").shouldBeInstanceOf<CheckoutResult.Failed>().error.shouldBeInstanceOf<SdkTransactionException>()
        }

    @Test fun `closed session that ran out before a payment fails as expired`() =
        runTest {
            val api = FakePaymentApi()
            api.nextSessionStatus = api.nextSessionStatus.copy(remainingSecs = null)
            // The gateway's whole answer for such an order: no status, only the flag (and a back URL).
            api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(sessionExpired = true)
            val controller = buildController(api = api).controller
            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            controller.state.value
                .shouldBeInstanceOf<PaymentState.Completed>()
                .result
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkSessionExpiredException>()
                .mdOrder shouldBe Fixtures.TEST_MD_ORDER
        }

    @Test fun `closed session the merchant declined fails with the gateway's code and reason`() =
        runTest {
            val api = FakePaymentApi()
            api.nextSessionStatus = api.nextSessionStatus.copy(remainingSecs = null)
            // An API V2 session the merchant expired (`POST /sessions/{id}/expire`): the gateway declines the order.
            api.nextFinishedPaymentInfo =
                FinishedPaymentInfoResponse(
                    status = "DECLINED",
                    actionCode = 4005,
                    actionCodeDescription = "Operation declined, please contact the store.",
                    displayErrorMessage = "Operation declined, please contact the store.",
                    errorTypeName = "DECLINED_BY_MERCHANT",
                )
            val controller = buildController(api = api).controller
            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            val error =
                controller.state.value
                    .shouldBeInstanceOf<PaymentState.Completed>()
                    .result
                    .shouldBeInstanceOf<CheckoutResult.Failed>()
                    .error
                    .shouldBeInstanceOf<SdkDeclinedException>()
            error.declineCode shouldBe "DECLINED_BY_MERCHANT"
            error.message shouldBe "Operation declined, please contact the store."
        }

    @Test fun `gateway refusal in processform ends with its reason without polling`() =
        runTest {
            val api = FakePaymentApi()
            api.nextProcessFormResponse =
                ProcessFormResponse(
                    errorCode = 0,
                    error = "Operation declined",
                    displayErrorMessage = "Operation declined, please check your details and try again.",
                    errorTypeName = "DATA_INPUT_ERROR",
                )
            val controller = buildController(api = api).controller
            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = null,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            val completed = controller.state.value.shouldBeInstanceOf<PaymentState.Completed>()
            val error =
                completed.result
                    .shouldBeInstanceOf<CheckoutResult.Failed>()
                    .error
                    .shouldBeInstanceOf<SdkDeclinedException>()
            error.declineCode shouldBe "DATA_INPUT_ERROR"
            completed.receipt?.actionCodeDescription shouldBe "Operation declined, please check your details and try again."
            api.getFinishedPaymentInfoCalls.shouldBeEmpty()
        }

    @Test fun `happy path new card without 3DS finishes successfully`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            val api = result.api
            api.nextProcessFormResponse = ProcessFormResponse(errorCode = 0, acsUrl = null, threeDSMethodURL = null)
            api.nextFinishedPaymentInfo = api.nextFinishedPaymentInfo.copy(status = "APPROVED")

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    private suspend fun TestScope.finishedWith(info: FinishedPaymentInfoResponse): CheckoutResult {
        val built = buildController()
        built.api.nextProcessFormResponse = ProcessFormResponse(errorCode = 0, acsUrl = null, threeDSMethodURL = null)
        built.api.nextFinishedPaymentInfo = info
        built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
        advanceUntilIdle()
        built.controller.handle(
            PaymentIntent.SubmitNewCard(
                pan = Fixtures.TEST_PAN,
                expiryMMYY = Fixtures.TEST_EXPIRY,
                cvc = Fixtures.TEST_CVC,
                cardHolder = Fixtures.TEST_CARDHOLDER,
                saveCard = false,
            ),
        )
        advanceUntilIdle()
        return built.controller.state.value
            .shouldBeInstanceOf<PaymentState.Completed>()
            .result
    }

    @Test fun `finished payment still CREATED without an action code is unknown`() =
        runTest {
            finishedWith(FinishedPaymentInfoResponse(status = "CREATED", actionCode = 0))
                .shouldBeInstanceOf<CheckoutResult.Unknown>()
                .orderId shouldBe Fixtures.TEST_MD_ORDER
        }

    @Test fun `finished payment DECLINED without an action code fails`() =
        runTest {
            finishedWith(FinishedPaymentInfoResponse(status = "DECLINED", actionCode = 0))
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkDeclinedException>()
        }

    @Test fun `session ran out before the payment reached the gateway - failed as expired and not unknown`() =
        runTest {
            val built = buildController()
            // processform.do on an expired session declines the order and answers with errorCode 0 and nothing else.
            built.api.nextProcessFormResponse = ProcessFormResponse(errorCode = 0)
            built.api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(sessionExpired = true)
            submitCard(built)
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkSessionExpiredException>()
                .mdOrder shouldBe Fixtures.TEST_MD_ORDER
            // Expired is final: no recheck.
            built.api.getFinishedPaymentInfoCalls.size shouldBe 1
            // The order is declined: there is no form to go back to and pay again.
            built.controller.canReturnToForm shouldBe false
        }

    @Test fun `3DS timed out and the session ran out - failed as expired`() =
        runTest {
            val built = buildController(webHost = FakeWebChallengeHost(result = ThreeDsResult.TimedOut))
            built.api.nextProcessFormResponse = threeDs
            built.api.finishedPaymentInfoQueue.add(Result.success(FinishedPaymentInfoResponse(sessionExpired = true)))
            submitCard(built)
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkSessionExpiredException>()
        }

    private fun info(status: String) = Result.success(FinishedPaymentInfoResponse(status = status, actionCode = 0))

    private fun offline() = Result.failure<FinishedPaymentInfoResponse>(SdkNetworkException("offline"))

    private val threeDs = ProcessFormResponse(errorCode = 0, acsUrl = "https://acs", paReq = "PA", termUrl = "https://t")

    /** Starts the checkout and submits a new card; the caller sets the fakes first and advances time itself. */
    private fun TestScope.submitCard(built: BuildResult) {
        built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
        advanceUntilIdle()
        built.controller.handle(
            PaymentIntent.SubmitNewCard(
                pan = Fixtures.TEST_PAN,
                expiryMMYY = Fixtures.TEST_EXPIRY,
                cvc = Fixtures.TEST_CVC,
                cardHolder = Fixtures.TEST_CARDHOLDER,
                saveCard = false,
            ),
        )
    }

    private fun BuildResult.terminal(): CheckoutResult =
        controller.state.value
            .shouldBeInstanceOf<PaymentState.Completed>()
            .result

    @Test fun `network lost after submitting the card and the order turns out paid - completed`() =
        runTest {
            val built = buildController()
            built.api.processFormError = SdkNetworkException("offline")
            built.api.finishedPaymentInfoQueue.addAll(listOf(offline(), info("APPROVED")))
            submitCard(built)
            advanceUntilIdle()

            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
            built.api.getFinishedPaymentInfoCalls.size shouldBe 2
        }

    @Test fun `network lost after submitting and the status never arrives - unknown`() =
        runTest {
            val built = buildController()
            built.api.processFormError = SdkNetworkException("offline")
            built.api.finishedPaymentInfoError = SdkNetworkException("still offline")
            submitCard(built)
            advanceUntilIdle()

            val unknown = built.terminal().shouldBeInstanceOf<CheckoutResult.Unknown>()
            unknown.orderId shouldBe Fixtures.TEST_MD_ORDER
            unknown.cause.shouldBeInstanceOf<SdkNetworkException>()
            built.api.getFinishedPaymentInfoCalls.size shouldBe 3
        }

    @Test fun `the recheck waits between attempts`() =
        runTest {
            val built = buildController()
            built.api.processFormError = SdkNetworkException("offline")
            built.api.finishedPaymentInfoError = SdkNetworkException("still offline")
            submitCard(built)
            runCurrent()
            built.api.getFinishedPaymentInfoCalls.size shouldBe 0
            advanceTimeBy(999)
            runCurrent()
            built.api.getFinishedPaymentInfoCalls.size shouldBe 0
            advanceTimeBy(1)
            runCurrent()
            built.api.getFinishedPaymentInfoCalls.size shouldBe 1
            advanceUntilIdle()
        }

    @Test fun `status still CREATED after the challenge is rechecked before unknown`() =
        runTest {
            val built = buildController()
            built.api.nextProcessFormResponse = threeDs
            built.api.finishedPaymentInfoQueue.addAll(listOf(info("CREATED"), info("CREATED"), info("DEPOSITED")))
            submitCard(built)
            advanceUntilIdle()

            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `3DS timed out but the bank approved - completed`() =
        runTest {
            val built = buildController(webHost = FakeWebChallengeHost(result = ThreeDsResult.TimedOut))
            built.api.nextProcessFormResponse = threeDs
            built.api.finishedPaymentInfoQueue.add(info("APPROVED"))
            submitCard(built)
            advanceUntilIdle()

            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `3DS timed out and the order is declined - failed with the timeout`() =
        runTest {
            val built = buildController(webHost = FakeWebChallengeHost(result = ThreeDsResult.TimedOut))
            built.api.nextProcessFormResponse = threeDs
            built.api.finishedPaymentInfoQueue.add(info("DECLINED"))
            submitCard(built)
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkThreeDsTimeoutException>()
        }

    @Test fun `3DS page lost its connection but the bank approved - completed`() =
        runTest {
            val built = buildController(webHost = FakeWebChallengeHost(result = ThreeDsResult.Interrupted("web-resource-error: -2")))
            built.api.nextProcessFormResponse = threeDs
            built.api.finishedPaymentInfoQueue.add(info("APPROVED"))
            submitCard(built)
            advanceUntilIdle()

            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `3DS page lost its connection and the order is declined - failed with the page error`() =
        runTest {
            val built = buildController(webHost = FakeWebChallengeHost(result = ThreeDsResult.Interrupted("web-resource-error: -2")))
            built.api.nextProcessFormResponse = threeDs
            built.api.finishedPaymentInfoQueue.add(info("DECLINED"))
            submitCard(built)
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkThreeDsChallengeFailedException>()
                .reason shouldBe "web-resource-error: -2"
        }

    @Test fun `3DS page lost its connection and the status never arrives - unknown`() =
        runTest {
            val built = buildController(webHost = FakeWebChallengeHost(result = ThreeDsResult.Interrupted("web-resource-error: -2")))
            built.api.nextProcessFormResponse = threeDs
            built.api.finishedPaymentInfoError = SdkNetworkException("offline")
            submitCard(built)
            advanceUntilIdle()

            built.terminal().shouldBeInstanceOf<CheckoutResult.Unknown>()
        }

    /** The gateway's answer for an order with no completed payment attempt while its session is alive. */
    private fun awaitingAttempt() =
        Result.success(
            FinishedPaymentInfoResponse(status = "CREATED", actionCode = -100, actionCodeDescription = "Operation pending or in progress."),
        )

    @Test fun `after the challenge the order still awaits an attempt - rechecked then unknown and not a decline`() =
        runTest {
            val built = buildController()
            built.api.nextProcessFormResponse = threeDs
            built.api.nextFinishedPaymentInfo = awaitingAttempt().getOrThrow()
            submitCard(built)
            advanceUntilIdle()

            built.terminal().shouldBeInstanceOf<CheckoutResult.Unknown>()
            built.api.getFinishedPaymentInfoCalls.size shouldBe 4
        }

    @Test fun `network lost after submitting and no attempt was recorded - failed with the network error`() =
        runTest {
            val built = buildController()
            built.api.processFormError = SdkNetworkException("offline")
            built.api.finishedPaymentInfoQueue.add(awaitingAttempt())
            submitCard(built)
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkNetworkException>()
            built.api.getFinishedPaymentInfoCalls.size shouldBe 1
        }

    @Test fun `3DS page lost its connection and no attempt was recorded - failed with the page error at once`() =
        runTest {
            val built = buildController(webHost = FakeWebChallengeHost(result = ThreeDsResult.Interrupted("web-resource-error: -2")))
            built.api.nextProcessFormResponse = threeDs
            built.api.nextFinishedPaymentInfo = awaitingAttempt().getOrThrow()
            submitCard(built)
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkThreeDsChallengeFailedException>()
            built.api.getFinishedPaymentInfoCalls.size shouldBe 1
        }

    @Test fun `codes for an outcome the gateway does not know yet are rechecked then unknown and not a decline`() =
        runTest {
            // Publicly documented: operation pending, response status unknown, request timed out, sent but no answer.
            for (code in listOf(-30001, 90, 998, 151019)) {
                val built = buildController()
                built.api.nextProcessFormResponse = ProcessFormResponse(errorCode = 0, acsUrl = null, threeDSMethodURL = null)
                built.api.nextFinishedPaymentInfo =
                    FinishedPaymentInfoResponse(status = "CREATED", actionCode = code, actionCodeDescription = "code $code")
                submitCard(built)
                advanceUntilIdle()

                built.terminal().shouldBeInstanceOf<CheckoutResult.Unknown>()
                built.api.getFinishedPaymentInfoCalls.size shouldBe 4
            }
        }

    @Test fun `an outcome the gateway did not know yet settles on a recheck`() =
        runTest {
            val built = buildController()
            built.api.nextProcessFormResponse = ProcessFormResponse(errorCode = 0, acsUrl = null, threeDSMethodURL = null)
            built.api.finishedPaymentInfoQueue.addAll(
                listOf(
                    Result.success(
                        FinishedPaymentInfoResponse(
                            status = "CREATED",
                            actionCode = 151019,
                            actionCodeDescription = "No answer from the bank",
                        ),
                    ),
                    info("DEPOSITED"),
                ),
            )
            submitCard(built)
            advanceUntilIdle()

            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `an HTTP error after submitting is rechecked - completed when the order turns out paid`() =
        runTest {
            val built = buildController()
            built.api.processFormError = SdkPaymentApiException(httpStatus = 504, message = "Gateway timeout")
            built.api.finishedPaymentInfoQueue.add(info("APPROVED"))
            submitCard(built)
            advanceUntilIdle()

            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `an unreadable answer after submitting is rechecked - failed when the order is declined`() =
        runTest {
            val built = buildController()
            built.api.processFormError = SdkPaymentApiException(message = "Unexpected JSON")
            built.api.finishedPaymentInfoQueue.add(info("DECLINED"))
            submitCard(built)
            advanceUntilIdle()

            built.terminal().shouldBeInstanceOf<CheckoutResult.Failed>()
            built.api.getFinishedPaymentInfoCalls.size shouldBe 1
        }

    @Test fun `network lost after paying with a saved card - rechecked`() =
        runTest {
            val built = buildController()
            built.api.processBindingFormError = SdkNetworkException("offline")
            built.api.finishedPaymentInfoQueue.add(info("DEPOSITED"))
            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            built.controller.handle(PaymentIntent.SubmitSavedCard(bindingId = "b-1", cvc = Fixtures.TEST_CVC))
            advanceUntilIdle()

            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `cancel while a sent payment is being rechecked is ignored`() =
        runTest {
            val built = buildController()
            built.api.processFormError = SdkNetworkException("offline")
            built.api.finishedPaymentInfoQueue.addAll(listOf(offline(), info("APPROVED")))
            submitCard(built)
            runCurrent()
            advanceTimeBy(500)
            built.controller.handle(PaymentIntent.Cancel)
            runCurrent()
            built.controller.state.value
                .shouldBeInstanceOf<PaymentState.Processing>()
            advanceUntilIdle()

            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `the first terminal result wins`() =
        runTest {
            // Cancel on the bank page, then the page reports the timeout: the checkout already ended as canceled.
            val gate = CompletableDeferred<ThreeDsResult>()
            val host =
                object : WebChallengeHost {
                    override suspend fun performChallenge(challenge: ThreeDsChallenge): ThreeDsResult = gate.await()
                }
            val api = FakePaymentApi().apply { nextProcessFormResponse = threeDs }
            val controller =
                PaymentFlowController(
                    api = api,
                    keyProvider = FakeKeyProvider(),
                    core = SdkCore(),
                    webChallengeHost = host,
                    logger = Logger(),
                    scope = this,
                    googlePayTokenProvider = FakeGooglePayTokenProvider(),
                    applePayTokenProvider = FakeApplePayTokenProvider(),
                )
            submitCard(BuildResult(controller, api, FakeWebChallengeHost(), FakeGooglePayTokenProvider(), FakeApplePayTokenProvider()))
            advanceUntilIdle()
            controller.handle(PaymentIntent.Cancel)
            advanceUntilIdle()
            gate.complete(ThreeDsResult.Failed("late"))
            advanceUntilIdle()

            controller.state.value
                .shouldBeInstanceOf<PaymentState.Completed>()
                .result shouldBe CheckoutResult.Canceled(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `after a successful challenge a lost status answer and no attempt recorded is unknown`() =
        runTest {
            val built = buildController()
            built.api.nextProcessFormResponse = threeDs
            built.api.finishedPaymentInfoQueue.add(offline())
            built.api.nextFinishedPaymentInfo = awaitingAttempt().getOrThrow()
            submitCard(built)
            advanceUntilIdle()

            built.terminal().shouldBeInstanceOf<CheckoutResult.Unknown>()
        }

    @Test fun `3DS timed out and the bank declined with a reason - failed with the gateway reason`() =
        runTest {
            val built = buildController(webHost = FakeWebChallengeHost(result = ThreeDsResult.TimedOut))
            built.api.nextProcessFormResponse = threeDs
            built.api.finishedPaymentInfoQueue.add(
                Result.success(
                    FinishedPaymentInfoResponse(
                        status = "CREATED",
                        actionCode = -2025,
                        actionCodeDescription = "Card issuer declined payment with RReq",
                    ),
                ),
            )
            submitCard(built)
            advanceUntilIdle()

            val error =
                built
                    .terminal()
                    .shouldBeInstanceOf<CheckoutResult.Failed>()
                    .error
                    .shouldBeInstanceOf<SdkDeclinedException>()
            error.declineCode shouldBe "-2025"
            error.message shouldBe "Card issuer declined payment with RReq"
        }

    @Test fun `retry is possible only when a form is there to return to`() =
        runTest {
            val failedAtStart = buildController()
            failedAtStart.api.sessionStatusError = SdkNetworkException("offline")
            failedAtStart.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            failedAtStart.controller.canReturnToForm shouldBe false

            val declined = buildController()
            declined.api.nextProcessFormResponse = ProcessFormResponse(errorCode = 0, error = "Declined", displayErrorMessage = "Declined")
            submitCard(declined)
            advanceUntilIdle()
            declined.controller.canReturnToForm shouldBe true
        }

    @Test fun `cancelling the scope mid-payment does not report a failure`() =
        runTest {
            val api = FakePaymentApi().apply { processFormGate = CompletableDeferred() }
            val flowScope = CoroutineScope(coroutineContext + Job())
            val controller =
                PaymentFlowController(
                    api = api,
                    keyProvider = FakeKeyProvider(),
                    core = SdkCore(),
                    webChallengeHost = FakeWebChallengeHost(),
                    logger = Logger(),
                    scope = flowScope,
                    googlePayTokenProvider = FakeGooglePayTokenProvider(),
                    applePayTokenProvider = FakeApplePayTokenProvider(),
                )
            submitCard(BuildResult(controller, api, FakeWebChallengeHost(), FakeGooglePayTokenProvider(), FakeApplePayTokenProvider()))
            runCurrent()
            flowScope.cancel()
            advanceUntilIdle()

            controller.state.value.shouldBeInstanceOf<PaymentState.Processing>()
        }

    @Test fun `after a restore a network error on start is unknown and not a failure`() =
        runTest {
            val built = buildController()
            built.controller.restored = true
            built.api.sessionStatusError = SdkNetworkException("offline")
            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            built.terminal().shouldBeInstanceOf<CheckoutResult.Unknown>().orderId shouldBe Fixtures.TEST_MD_ORDER
        }

    @Test fun `after a restore a closed session that was paid completes`() =
        runTest {
            val built = buildController()
            built.controller.restored = true
            built.api.nextSessionStatus = built.api.nextSessionStatus.copy(remainingSecs = null)
            built.api.nextFinishedPaymentInfo = FinishedPaymentInfoResponse(status = "DEPOSITED", actionCode = 0)
            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            built.terminal() shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `closed by the app on the form the checkout is canceled`() =
        runTest {
            val built = buildController()
            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            built.controller.resultIfClosedNow() shouldBe CheckoutResult.Canceled(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `closed by the app while a payment is in flight the outcome is unknown`() =
        runTest {
            val built = buildController()
            built.api.processFormGate = CompletableDeferred()
            submitCard(built)
            runCurrent()

            built.controller.resultIfClosedNow() shouldBe CheckoutResult.Unknown(Fixtures.TEST_MD_ORDER)
            built.api.processFormGate!!.complete(Unit)
            advanceUntilIdle()
        }

    @Test fun `closed by the app on a result screen the result is that one`() =
        runTest {
            val built = buildController()
            built.api.nextProcessFormResponse = ProcessFormResponse(errorCode = 0, error = "Declined", displayErrorMessage = "Declined")
            submitCard(built)
            advanceUntilIdle()

            built.controller.resultIfClosedNow() shouldBe built.terminal()
        }

    @Test fun `network lost while loading the session - failed and no recheck`() =
        runTest {
            val built = buildController()
            built.api.sessionStatusError = SdkNetworkException("offline")
            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            built
                .terminal()
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkNetworkException>()
            built.api.getFinishedPaymentInfoCalls.shouldBeEmpty()
        }

    @Test fun `happy path new card WITH 3DS finishes successfully after challenge succeeds`() =
        runTest {
            val webHost = FakeWebChallengeHost(result = ThreeDsResult.Succeeded(queryParams = mapOf("OK" to "1")))
            val result = buildController(webHost = webHost)
            val controller = result.controller
            val api = result.api
            val host = result.webHost
            api.nextProcessFormResponse =
                ProcessFormResponse(
                    errorCode = 0,
                    acsUrl = "https://acs/ok",
                    paReq = "PA",
                    termUrl = "https://t/end",
                )
            api.nextFinishedPaymentInfo = api.nextFinishedPaymentInfo.copy(status = "DEPOSITED")

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            host.performedChallenges.size shouldBe 1
            host.performedChallenges[0].acsUrl shouldBe "https://acs/ok"
            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `the bank page waits for the checkout to make room`() =
        runTest {
            val webHost = FakeWebChallengeHost(result = ThreeDsResult.Succeeded(queryParams = mapOf("OK" to "1")))
            val result = buildController(webHost = webHost)
            val controller = result.controller
            result.api.nextProcessFormResponse =
                ProcessFormResponse(errorCode = 0, acsUrl = "https://acs/ok", paReq = "PA", termUrl = "https://t/end")
            val roomMade = CompletableDeferred<Unit>()
            controller.beforeChallenge = { roomMade.await() }

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            controller.state.value shouldBe PaymentState.ThreeDsChallenge
            result.webHost.performedChallenges.size shouldBe 0

            roomMade.complete(Unit)
            advanceUntilIdle()
            result.webHost.performedChallenges.size shouldBe 1
        }

    @Test fun `cancelled while the checkout makes room - the bank page never opens`() =
        runTest {
            val webHost = FakeWebChallengeHost(result = ThreeDsResult.Succeeded(queryParams = mapOf("OK" to "1")))
            val result = buildController(webHost = webHost)
            val controller = result.controller
            result.api.nextProcessFormResponse =
                ProcessFormResponse(errorCode = 0, acsUrl = "https://acs/ok", paReq = "PA", termUrl = "https://t/end")
            val roomMade = CompletableDeferred<Unit>()
            controller.beforeChallenge = { roomMade.await() }

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()
            controller.handle(PaymentIntent.Cancel)
            advanceUntilIdle()
            roomMade.complete(Unit)
            advanceUntilIdle()

            result.webHost.performedChallenges.size shouldBe 0
            (controller.state.value as PaymentState.Completed).result shouldBe CheckoutResult.Canceled(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `a second tap on pay while the first is in flight sends nothing`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            val submit =
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                )
            controller.handle(submit)
            controller.handle(submit)
            advanceUntilIdle()

            result.api.processFormCalls.size shouldBe 1
        }

    @Test fun `gateway decline after 3DS ends with its reason`() =
        runTest {
            val webHost = FakeWebChallengeHost(result = ThreeDsResult.Succeeded(queryParams = emptyMap()))
            val result = buildController(webHost = webHost)
            val controller = result.controller
            val api = result.api
            api.nextProcessFormResponse =
                ProcessFormResponse(errorCode = 0, acsUrl = "https://acs/ok", paReq = "PA", termUrl = "https://t/end")
            api.nextFinishedPaymentInfo =
                FinishedPaymentInfoResponse(
                    status = "CREATED",
                    approvalCode = "null",
                    actionCode = 71015,
                    actionCodeDescription = "Operation declined, please check your details and try again.",
                    displayErrorMessage = "Operation declined, please check your details and try again.",
                    errorTypeName = "DATA_INPUT_ERROR",
                )

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = null,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            val completed = controller.state.value.shouldBeInstanceOf<PaymentState.Completed>()
            val error =
                completed.result
                    .shouldBeInstanceOf<CheckoutResult.Failed>()
                    .error
                    .shouldBeInstanceOf<SdkDeclinedException>()
            error.declineCode shouldBe "DATA_INPUT_ERROR"
            error.message shouldBe "Operation declined, please check your details and try again."
            completed.receipt?.actionCodeDescription shouldBe "Operation declined, please check your details and try again."
            completed.receipt?.approvalCode shouldBe null
        }

    @Test fun `result amount carries the session currency when the gateway writes it without one`() =
        runTest {
            val webHost = FakeWebChallengeHost(result = ThreeDsResult.Succeeded(queryParams = emptyMap()))
            val result = buildController(webHost = webHost)
            val controller = result.controller
            val api = result.api
            api.nextSessionStatus = api.nextSessionStatus.copy(amount = "32900", currencyAlphaCode = "EUR")
            api.nextProcessFormResponse =
                ProcessFormResponse(errorCode = 0, acsUrl = "https://acs/ok", paReq = "PA", termUrl = "https://t/end")
            api.nextFinishedPaymentInfo =
                FinishedPaymentInfoResponse(
                    status = "CREATED",
                    actionCode = 71015,
                    actionCodeDescription = "Operation declined, please check your details and try again.",
                    formattedAmount = "329.00",
                )

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = null,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            val completed = controller.state.value.shouldBeInstanceOf<PaymentState.Completed>()
            completed.receipt?.formattedAmount shouldBe controller.order.value?.formattedAmount
            completed.receipt?.formattedAmount?.contains("€") shouldBe true
        }

    @Test fun `threeDSMethodURL surfaces as SdkNotConfiguredException`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            val api = result.api
            api.nextProcessFormResponse =
                ProcessFormResponse(
                    errorCode = 0,
                    threeDSMethodURL = "https://3ds2/method",
                )

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkNotConfiguredException>()
        }

    @Test fun `3DS cancelled completes as canceled`() =
        runTest {
            val webHost = FakeWebChallengeHost(result = ThreeDsResult.Cancelled)
            val result = buildController(webHost = webHost)
            val controller = result.controller
            val api = result.api
            api.nextProcessFormResponse =
                ProcessFormResponse(
                    errorCode = 0,
                    acsUrl = "https://acs",
                    paReq = "PA",
                    termUrl = "https://t",
                )

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result shouldBe CheckoutResult.Canceled(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `Cancel intent completes as canceled`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            controller.handle(PaymentIntent.Cancel)
            advanceUntilIdle()

            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result shouldBe CheckoutResult.Canceled(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `session with SessionId config resolves to mdOrder via SessionIdConverter`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            val api = result.api
            val sessionId =
                net.payrdr.mobile.payment.sdk.payment.utils.SessionIdConverter
                    .mdOrderToSessionId(Fixtures.TEST_MD_ORDER)

            controller.handle(PaymentIntent.Start(CheckoutConfig.SessionId(sessionId)))
            advanceUntilIdle()

            api.getSessionStatusCalls shouldBe listOf(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `the bank page knows the session id the gateway returns to the merchant with`() =
        runTest {
            val sessionId =
                net.payrdr.mobile.payment.sdk.payment.utils.SessionIdConverter
                    .mdOrderToSessionId(Fixtures.TEST_MD_ORDER)
            for (checkout in listOf(CheckoutConfig.SessionId(sessionId), CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER))) {
                val built = buildController()
                built.api.nextProcessFormResponse = threeDs
                built.controller.handle(PaymentIntent.Start(checkout))
                advanceUntilIdle()
                built.controller.handle(
                    PaymentIntent.SubmitNewCard(
                        pan = Fixtures.TEST_PAN,
                        expiryMMYY = Fixtures.TEST_EXPIRY,
                        cvc = Fixtures.TEST_CVC,
                        cardHolder = Fixtures.TEST_CARDHOLDER,
                        saveCard = false,
                    ),
                )
                advanceUntilIdle()

                built.webHost.performedChallenges
                    .single()
                    .sessionId shouldBe sessionId
            }
        }

    @Test fun `TapGooglePay happy path no 3DS finishes successfully`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            val api = result.api
            val gpay = result.googlePay
            api.nextFinishedPaymentInfo = api.nextFinishedPaymentInfo.copy(status = "DEPOSITED")
            // api.nextGooglePayProcessFormResponse default: success="true", data=null → no 3DS path

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            controller.handle(
                PaymentIntent.TapGooglePay,
            )
            advanceUntilIdle()

            gpay.calls.size shouldBe 1
            api.googlePayCalls.size shouldBe 1
            // The gateway reads the token as base64, as its own payment page sends it.
            api.googlePayCalls[0].paymentToken shouldBe "RkFLRV9KV0VfVE9LRU4="
            api.googlePayCalls[0].mdOrder shouldBe Fixtures.TEST_MD_ORDER

            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `TapApplePay happy path no 3DS finishes successfully`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            val api = result.api
            val apay = result.applePay
            api.nextFinishedPaymentInfo = api.nextFinishedPaymentInfo.copy(status = "DEPOSITED")

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            controller.handle(
                PaymentIntent.TapApplePay,
            )
            advanceUntilIdle()

            apay.calls.size shouldBe 1
            api.applePayCalls.size shouldBe 1
            api.applePayCalls[0].paymentToken shouldBe "FAKE_APPLE_PAY_TOKEN_B64"
            api.applePayCalls[0].mdOrder shouldBe Fixtures.TEST_MD_ORDER

            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `wallet sheets get the amount the gateway sends as a formatted string`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            val api = result.api
            api.nextSessionStatus = api.nextSessionStatus.copy(amount = "1 249.90 EUR", currencyAlphaCode = "EUR")
            result.googlePay.nextResult = GooglePayTokenResult.Cancelled
            result.applePay.nextResult = ApplePayTokenResult.Cancelled

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.TapGooglePay,
            )
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.TapApplePay,
            )
            advanceUntilIdle()

            val gpay = result.googlePay
            val apay = result.applePay
            gpay.calls.size shouldBe 1
            gpay.calls[0].amountMinorUnits shouldBe 124990L
            apay.calls.size shouldBe 1
            apay.calls[0].amountMinorUnits shouldBe 124990L
        }

    @Test fun `TapGooglePay cancelled returns to card form without terminating checkout`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            val gpay = result.googlePay
            gpay.nextResult = GooglePayTokenResult.Cancelled

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            val stateBeforeTap = controller.state.value
            stateBeforeTap.shouldBeInstanceOf<PaymentState.CardFormShown>()

            controller.handle(
                PaymentIntent.TapGooglePay,
            )
            advanceUntilIdle()

            val stateAfterCancel = controller.state.value
            stateAfterCancel.shouldBeInstanceOf<PaymentState.CardFormShown>()
            result.api.googlePayCalls.shouldBeEmpty()
        }

    @Test fun `TapGooglePay NotAvailable terminates with SdkGooglePayException`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            val gpay = result.googlePay
            gpay.nextResult = GooglePayTokenResult.NotAvailable

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.TapGooglePay,
            )
            advanceUntilIdle()

            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkGooglePayException>()
        }

    @Test fun `TapGooglePay Failure terminates with SdkGooglePayException carrying reason`() =
        runTest {
            val result = buildController()
            val controller = result.controller
            val gpay = result.googlePay
            gpay.nextResult = GooglePayTokenResult.Failure(reason = "GPay sheet crashed", cause = null)

            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.TapGooglePay,
            )
            advanceUntilIdle()

            val terminal = controller.state.value
            terminal.shouldBeInstanceOf<PaymentState.Completed>()
            terminal.result
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkGooglePayException>()
                .message shouldBe "GPay sheet crashed"
        }

    @Test fun `wallets show only when the gateway has enabled them for the merchant`() =
        runTest {
            val built = buildController()
            built.api.nextSessionStatus = built.api.nextSessionStatus.copy(merchantOptions = listOf("CARD"))

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            val form =
                built.controller.state.value
                    .shouldBeInstanceOf<PaymentState.CardFormShown>()
            form.googlePay.shouldBeNull()
            form.applePay.shouldBeNull()
            built.api.getPaymentSettingsCalls.shouldBeEmpty()
        }

    @Test fun `a failed settings request leaves google pay off and the card form on`() =
        runTest {
            val built = buildController()
            built.api.paymentSettingsError = SdkNetworkException("offline")

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            val form =
                built.controller.state.value
                    .shouldBeInstanceOf<PaymentState.CardFormShown>()
            form.googlePay.shouldBeNull()
            form.applePay.shouldNotBeNull()
        }

    @Test fun `after a failed settings request a card payment still completes`() =
        runTest {
            val built = buildController()
            built.api.paymentSettingsError = SdkNetworkException("offline")
            built.api.nextFinishedPaymentInfo = built.api.nextFinishedPaymentInfo.copy(status = "DEPOSITED")

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            built.controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = null,
                    saveCard = false,
                ),
            )
            advanceUntilIdle()

            built.controller.state.value
                .shouldBeInstanceOf<PaymentState.Completed>()
                .result shouldBe CheckoutResult.Completed(Fixtures.TEST_MD_ORDER)
        }

    @Test fun `without a wallet in the config the gateway settings are not asked for`() =
        runTest {
            val built = buildController(googlePayConfig = null, applePayConfig = null)

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()

            built.api.getPaymentSettingsCalls.shouldBeEmpty()
            built.controller.state.value
                .shouldBeInstanceOf<PaymentState.CardFormShown>()
                .googlePay
                .shouldBeNull()
        }

    @Test fun `the google pay sheet gets the gateway settings with the order currency and the merchant country and login`() =
        runTest {
            val built = buildController()
            built.googlePay.nextResult = GooglePayTokenResult.Cancelled

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            built.controller.handle(PaymentIntent.TapGooglePay)
            advanceUntilIdle()

            built.api.getPaymentSettingsCalls shouldBe listOf("m-login")
            val call = built.googlePay.calls.single()
            call.currencyCode shouldBe "EUR"
            call.countryCode shouldBe "DE"
            call.config.gateway shouldBe "bpcpay"
            call.config.merchantId shouldBe "M-1"
            call.config.gatewayMerchantId shouldBe "m-login"
            call.config.merchantFullName shouldBe "Test Merchant"
        }

    @Test fun `the apple pay sheet gets the merchant country and the order currency`() =
        runTest {
            val built = buildController()
            built.applePay.nextResult = ApplePayTokenResult.Cancelled

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            built.controller.handle(PaymentIntent.TapApplePay)
            advanceUntilIdle()

            val call = built.applePay.calls.single()
            call.countryCode shouldBe "DE"
            call.currencyCode shouldBe "EUR"
        }

    @Test fun `a tap on a wallet the order does not offer keeps the card form`() =
        runTest {
            val built = buildController()
            built.api.nextSessionStatus = built.api.nextSessionStatus.copy(merchantOptions = listOf("CARD"))

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            built.controller.handle(PaymentIntent.TapApplePay)
            advanceUntilIdle()

            built.applePay.calls.shouldBeEmpty()
            built.controller.state.value
                .shouldBeInstanceOf<PaymentState.CardFormShown>()
        }

    @Test fun `a wallet refusal with an empty buyer message ends with the gateway reason`() =
        runTest {
            val built = buildController()
            built.api.nextApplePayProcessFormResponse =
                ApplePayProcessFormResponse(
                    success = false,
                    error = WalletError(code = "10", message = "Bad [paymentToken]", displayErrorMessage = ""),
                )

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            built.controller.handle(PaymentIntent.TapApplePay)
            advanceUntilIdle()

            built.api.getFinishedPaymentInfoCalls.shouldBeEmpty()
            built.controller.state.value
                .shouldBeInstanceOf<PaymentState.Completed>()
                .result
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .message shouldBe "Bad [paymentToken]"
        }

    @Test fun `hanging payment settings do not hold the card form`() =
        runTest {
            val built = buildController()
            built.api.paymentSettingsDelayMs = 60_000

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceTimeBy(5_000)
            runCurrent()

            val form =
                built.controller.state.value
                    .shouldBeInstanceOf<PaymentState.CardFormShown>()
            form.googlePay.shouldBeNull()
            form.applePay.shouldNotBeNull()
        }

    @Test fun `an apple pay refusal ends with the gateway reason and not as unknown`() =
        runTest {
            val built = buildController()
            built.api.nextApplePayProcessFormResponse =
                ApplePayProcessFormResponse(success = false, error = WalletError(code = "10", message = "Bad [paymentToken]"))

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            built.controller.handle(PaymentIntent.TapApplePay)
            advanceUntilIdle()

            built.api.getFinishedPaymentInfoCalls.shouldBeEmpty()
            built.controller.state.value
                .shouldBeInstanceOf<PaymentState.Completed>()
                .result
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .shouldBeInstanceOf<SdkDeclinedException>()
                .message shouldBe "Bad [paymentToken]"
        }

    @Test fun `a google pay refusal ends with the gateway reason and not as unknown`() =
        runTest {
            val built = buildController()
            built.api.nextGooglePayProcessFormResponse =
                ProcessFormGPayResponse(
                    success = false,
                    error = WalletError(code = "9", message = "Wrong order state", displayErrorMessage = "Try again"),
                )

            built.controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            built.controller.handle(PaymentIntent.TapGooglePay)
            advanceUntilIdle()

            built.api.getFinishedPaymentInfoCalls.shouldBeEmpty()
            built.controller.state.value
                .shouldBeInstanceOf<PaymentState.Completed>()
                .result
                .shouldBeInstanceOf<CheckoutResult.Failed>()
                .error
                .message shouldBe "Try again"
        }

    @Test fun `failed card payment logs no card data`() =
        runTest {
            val events = mutableListOf<LogEvent>()
            val controller =
                PaymentFlowController(
                    api = FakePaymentApi(),
                    keyProvider =
                        object : KeyProvider {
                            override suspend fun provideKey(): Key = throw IllegalStateException("key server down")
                        },
                    core = SdkCore(),
                    webChallengeHost = FakeWebChallengeHost(),
                    logger = Logger(listOf(LogTransport { events += it })),
                    scope = this,
                    googlePayTokenProvider = FakeGooglePayTokenProvider(),
                    applePayTokenProvider = FakeApplePayTokenProvider(),
                )
            controller.handle(PaymentIntent.Start(CheckoutConfig.MdOrder(Fixtures.TEST_MD_ORDER)))
            advanceUntilIdle()
            controller.handle(
                PaymentIntent.SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = false,
                    payerData = mapOf(PayerField.EMAIL to "alex.morgan@example.com"),
                ),
            )
            advanceUntilIdle()

            controller.state.value.shouldBeInstanceOf<PaymentState.Completed>()
            events.shouldNotBeEmpty()
            events.forEach { event ->
                event.message shouldNotContain Fixtures.TEST_PAN
                event.message shouldNotContain Fixtures.TEST_CVC
                event.message shouldNotContain Fixtures.TEST_EXPIRY
                event.message shouldNotContain Fixtures.TEST_CARDHOLDER
                event.message shouldNotContain "alex.morgan@example.com"
            }
        }
}
