package net.payrdr.mobile.payment.sdk.payment.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ApplePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.GooglePayProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.P2PFromCard
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.P2PPerformRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.ProcessFormRequest
import net.payrdr.mobile.payment.sdk.payment.api.dto.request.WalletVendor
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNetworkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.result.PayerField
import net.payrdr.mobile.payment.sdk.payment.support.json
import net.payrdr.mobile.payment.sdk.payment.support.methodAndPath
import net.payrdr.mobile.payment.sdk.payment.support.mockJsonClient
import kotlin.test.Test

private const val BASE_URL = "https://example.com"

class PaymentApiImplTest {
    @Test fun `getSessionStatus POSTs to correct path and parses response`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json("""{"remainingSecs":600,"orderNumber":"O1","merchantInfo":{"merchantLogin":"L","merchantFullName":"N"}}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val response = api.getSessionStatus(mdOrder = "ORDER-1")

            recorded.single().methodAndPath() shouldBe (HttpMethod.Post to "/rest/getSessionStatus.do")
            val body =
                recorded
                    .single()
                    .body
                    .toByteArray()
                    .decodeToString()
            body shouldContain "MDORDER=ORDER-1"
            response.orderNumber shouldBe "O1"
        }

    @Test fun `getSessionStatus wraps http error as SdkPaymentApiException`() =
        runTest {
            val (client, _) = mockJsonClient { _ -> json("""{"err":"oops"}""", HttpStatusCode.InternalServerError) }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val ex = shouldThrow<SdkPaymentApiException> { api.getSessionStatus("ORDER-1") }
            ex.httpStatus shouldBe 500
        }

    @Test fun `a transport failure without a response is a network error`() =
        runTest {
            val (client, _) = mockJsonClient { _ -> throw IOException("unreachable") }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            shouldThrow<SdkNetworkException> { api.getSessionStatus("ORDER-1") }
        }

    @Test fun `an unreadable body is an API error and not a network error`() =
        runTest {
            val (client, _) = mockJsonClient { _ -> json("not json") }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            shouldThrow<SdkPaymentApiException> { api.getSessionStatus("ORDER-1") }
        }

    @Test fun `processForm sends expected body fields`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json("""{"errorCode":0,"acsUrl":null}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            api.processForm(
                ProcessFormRequest(
                    paymentToken = "TOK-1",
                    mdOrder = "ORDER-1",
                    holder = "JOHN",
                    saveCard = true,
                    payerData =
                        mapOf(
                            PayerField.EMAIL to "i@x.com",
                            PayerField.MOBILE_PHONE to "+44 20 7946 0958",
                            PayerField.BILLING_CITY to "London",
                            PayerField.BILLING_COUNTRY to "GB",
                            PayerField.BILLING_STATE to " ",
                        ),
                ),
            )

            val body =
                recorded
                    .single()
                    .body
                    .toByteArray()
                    .decodeToString()
            recorded.single().url.encodedPath shouldBe "/rest/processform.do"
            body shouldContain "seToken=TOK-1"
            body shouldContain "MDORDER=ORDER-1"
            body shouldContain "TEXT=JOHN"
            body shouldContain "bindingNotNeeded=false"
            body shouldContain "threeDSSDK=false"
            body shouldContain "email=i%40x.com"
            val params = parseFormBody(body)
            params["orderPayerData"] shouldBe """{"mobilePhone":"+442079460958"}"""
            params["billingPayerData"] shouldBe """{"billingCountry":"GB","billingCity":"London"}"""
        }

    @Test fun `processForm response with 3DS challenge parses correctly`() =
        runTest {
            val (client, _) =
                mockJsonClient { _ ->
                    json("""{"errorCode":0,"acsUrl":"https://acs","paReq":"PA","termUrl":"https://t"}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val r = api.processForm(ProcessFormRequest("TOK", "O", "H", false))

            r.acsUrl shouldBe "https://acs"
            r.paReq shouldBe "PA"
            r.termUrl shouldBe "https://t"
        }

    @Test fun `processBindingForm hits binding endpoint`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json("""{"errorCode":0}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            api.processBindingForm(ProcessFormRequest("TOK", "O", "H", false))

            recorded.single().url.encodedPath shouldBe "/rest/processBindingForm.do"
        }

    @Test fun `googlePayProcessForm sends JSON body`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json("""{"success":"true"}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            api.googlePayProcessForm(GooglePayProcessFormRequest(paymentToken = "G-TOK", mdOrder = "O-1"))

            val body =
                recorded
                    .single()
                    .body
                    .toByteArray()
                    .decodeToString()
            recorded.single().url.encodedPath shouldBe "/google/paymentOrder.do"
            body shouldContain "\"paymentToken\":\"G-TOK\""
            body shouldContain "\"mdOrder\":\"O-1\""
        }

    private suspend fun p2pBody(recorded: List<HttpRequestData>): kotlinx.serialization.json.JsonObject =
        kotlinx.serialization.json.Json
            .parseToJsonElement(
                recorded
                    .single()
                    .body
                    .toByteArray()
                    .decodeToString(),
            ).let { it as kotlinx.serialization.json.JsonObject }

    @Test fun `p2pPerform posts the transfer as JSON the way the gateway's page does`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json(
                        """{"errorCode":0,"info":"Your order is proceeded, redirecting...","acsUrl":"https://gw/getwhitepageurl.do","paReq":"White page paReq","termUrl":"White page termUrl","is3DSVer2":false}""",
                    )
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val response =
                api.p2pPerform(
                    P2PPerformRequest(
                        mdOrder = "O-1",
                        fromCard = P2PFromCard.SeToken("SE-TOK"),
                        bindingNotNeeded = true,
                        payerData =
                            mapOf(
                                PayerField.EMAIL to "a@b.c",
                                PayerField.MOBILE_PHONE to "+357 99 123456",
                                PayerField.BILLING_CITY to "Nicosia",
                                PayerField.PAYER_FIRST_NAME to "Test",
                                PayerField.PAYER_BIRTHDAY to "19901212",
                                PayerField.RECIPIENT_ACCOUNT to "acc-1",
                                PayerField.RECIPIENT_COUNTRY to "CY",
                            ),
                        language = "en",
                    ),
                )

            recorded.single().methodAndPath() shouldBe (HttpMethod.Post to "/rest/p2p/perform.do")
            val body = p2pBody(recorded).toString()
            body shouldContain "\"orderId\":\"O-1\""
            body shouldContain "\"fromCard\":{\"seToken\":\"SE-TOK\"}"
            body shouldContain "\"bindingNotNeeded\":true"
            body shouldContain
                "\"billingPayerData\":{\"billingCity\":\"Nicosia\",\"payerFirstName\":\"Test\",\"payerBirthday\":\"19901212\"}"
            body shouldContain "\"billingRecipientData\":{\"recipientAccount\":\"acc-1\",\"recipientCountry\":\"CY\"}"
            body shouldContain "\"email\":\"a@b.c\""
            body shouldContain "\"orderPayerData\":{\"mobilePhone\":\"+35799123456\"}"
            body shouldContain "\"language\":\"en\""
            // An amount would change the order's: the transfer takes the registered one.
            body shouldNotContain "amountInput"
            response.acsUrl shouldBe "https://gw/getwhitepageurl.do"
            response.termUrl shouldBe "White page termUrl"
        }

    @Test fun `p2pPerform sends a wallet token with its vendor and nothing else of a card`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json("""{"errorCode":0}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            api.p2pPerform(
                P2PPerformRequest(mdOrder = "O-1", fromCard = P2PFromCard.Wallet(WalletVendor.GOOGLE, "R1RPSw=="), bindingNotNeeded = true),
            )

            recorded.single().url.encodedPath shouldBe "/rest/p2p/perform.do"
            val body = p2pBody(recorded).toString()
            // The gateway refuses a wallet token beside a seToken, a PAN, an expiry, a CVC or a binding id.
            body shouldContain "\"fromCard\":{\"vendor\":\"GOOGLE\",\"paymentToken\":\"R1RPSw==\"}"
            body shouldContain "\"bindingNotNeeded\":true"
        }

    @Test fun `p2pPerformByBinding sends the binding id and leaves empty data out`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json("""{"errorCode":0}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            api.p2pPerformByBinding(P2PPerformRequest(mdOrder = "O-1", fromCard = P2PFromCard.Binding("B-1"), bindingNotNeeded = true))

            recorded.single().url.encodedPath shouldBe "/rest/p2p/performByBinding.do"
            val body = p2pBody(recorded).toString()
            body shouldContain "\"fromCard\":{\"bindingId\":\"B-1\"}"
            body shouldNotContain "billingPayerData"
            body shouldNotContain "billingRecipientData"
        }

    @Test fun `a refused transfer is read with the gateway's errorMessage`() =
        runTest {
            val (client, _) = mockJsonClient { _ -> json("""{"errorCode":"7","errorMessage":"System error"}""") }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val response = api.p2pPerform(P2PPerformRequest(mdOrder = "O-1", fromCard = P2PFromCard.SeToken("T"), bindingNotNeeded = true))

            response.errorCode shouldBe 7
            response.error shouldBe "System error"
        }

    @Test fun `a transfer paid without the bank page is not read as refused by its Successful message`() =
        runTest {
            val (client, _) = mockJsonClient { _ -> json("""{"errorCode":0,"errorMessage":"Successful"}""") }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val response = api.p2pPerform(P2PPerformRequest(mdOrder = "O-1", fromCard = P2PFromCard.SeToken("T"), bindingNotNeeded = true))

            response.errorCode shouldBe 0
            response.error shouldBe null
        }

    @Test fun `applePayProcessForm sends JSON body`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json("""{"success":"true"}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            api.applePayProcessForm(ApplePayProcessFormRequest(paymentToken = "A-TOK", mdOrder = "O-1"))

            // `payment.do` registers a new order (it asks for `merchant`); an order the merchant registered is paid here.
            recorded.single().url.encodedPath shouldBe "/applepay/paymentOrder.do"
            recorded
                .single()
                .body
                .toByteArray()
                .decodeToString() shouldContain "A-TOK"
        }

    @Test fun `a wallet refusal is read with the gateway's error`() =
        runTest {
            val (client, _) =
                mockJsonClient { _ ->
                    json(
                        """{"success":false,"data":{"redirectUrl":"https://pay.example.com/finish.html?orderId=O-1"},""" +
                            """"error":{"code":"10","description":"Bad [paymentToken]","message":"Bad [paymentToken]","displayErrorMessage":""}}""",
                    )
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val response = api.applePayProcessForm(ApplePayProcessFormRequest(paymentToken = "A-TOK", mdOrder = "O-1"))

            response.success shouldBe false
            response.error?.code shouldBe "10"
            response.error?.message shouldBe "Bad [paymentToken]"
        }

    @Test fun `getFinishedPaymentInfo parses status`() =
        runTest {
            val (client, _) =
                mockJsonClient { _ ->
                    json("""{"status":"APPROVED","paymentDate":"2026-04-22","panMasked":"**** 9077"}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val r = api.getFinishedPaymentInfo(orderId = "O-1")

            r.status shouldBe "APPROVED"
            r.paymentDate shouldBe "2026-04-22"
            r.panMasked shouldBe "**** 9077"
        }

    @Test fun `getFinishedPaymentInfo of an order whose session ran out reads the flag`() =
        runTest {
            // The gateway's whole answer for an order declined because its session expired before a payment.
            val (client, _) =
                mockJsonClient { _ ->
                    json("""{"sessionExpired":true,"backUrl":"https://shop.example/fail","isWebView":true}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val r = api.getFinishedPaymentInfo(orderId = "O-1")

            r.sessionExpired shouldBe true
            r.status shouldBe null
        }

    @Test fun `getPaymentSettings is a GET with login query param and reads every setting`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json(
                        """{"settings":{"googlePay.environment":"TEST","googlePay.merchantId":"M-1","googlePay.gateway":"bpcpay",""" +
                            """"ecom2.hidden.payments":"SBP_PAY, APPLE_PAY"}}""",
                    )
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            val settings = api.getPaymentSettings(login = "merchant-login")

            recorded.single().methodAndPath() shouldBe (HttpMethod.Get to "/rest/getPaymentSettings.do")
            recorded.single().url.parameters["login"] shouldBe "merchant-login"
            settings.googlePayEnvironment shouldBe "TEST"
            settings.googlePayMerchantId shouldBe "M-1"
            settings.googlePayGateway shouldBe "bpcpay"
            settings.hiddenPayments shouldBe setOf("SBP_PAY", "APPLE_PAY")
        }

    @Test fun `getPaymentSettings for a merchant without settings is empty`() =
        runTest {
            val (client, _) = mockJsonClient { _ -> json("""{"error":"Data not found"}""") }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            api.getPaymentSettings(login = "merchant-login").values shouldBe emptyMap()
        }

    @Test fun `unbindCardAnonymous posts binding id and order`() =
        runTest {
            val recorded = mutableListOf<HttpRequestData>()
            val (client, _) =
                mockJsonClient { req ->
                    recorded += req
                    json("""{"errorCode":0}""")
                }
            val api = PaymentApiImpl(baseUrl = BASE_URL, httpClient = client)

            api.unbindCardAnonymous(bindingId = "B-1", mdOrder = "O-1")

            val body =
                recorded
                    .single()
                    .body
                    .toByteArray()
                    .decodeToString()
            recorded.single().url.encodedPath shouldBe "/rest/unbindcardanon.do"
            body shouldContain "bindingId=B-1"
            body shouldContain "mdOrder=O-1"
        }

    private fun parseFormBody(body: String): Map<String, String> =
        body.split("&").associate { pair ->
            val (k, v) = pair.split("=", limit = 2)
            k.decodeURLQueryComponent(plusIsSpace = true) to v.decodeURLQueryComponent(plusIsSpace = true)
        }
}
