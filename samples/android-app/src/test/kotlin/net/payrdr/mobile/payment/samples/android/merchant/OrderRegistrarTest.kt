package net.payrdr.mobile.payment.samples.android.merchant

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.payrdr.mobile.payment.samples.android.store.invest.NorthwindProfile
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OrderRegistrarTest {
    private val baseUrl = "https://gateway.example/payment"
    private val requests = mutableListOf<HttpRequestData>()

    private fun http(body: String = """{"orderId":"md-1","formUrl":"https://x"}""") =
        HttpClient(
            MockEngine { request ->
                requests += request
                respond(body, HttpStatusCode.OK)
            },
        )

    private fun HttpRequestData.form(): Parameters = (body as FormDataContent).formData

    @Test fun `parse success`() {
        assertEquals(
            OrderRegistration.Registered("0154d1a2-c70e"),
            parseRegisterResponse("""{"orderId":"0154d1a2-c70e","formUrl":"https://x"}"""),
        )
    }

    @Test fun `parse gateway error`() {
        assertEquals(
            OrderRegistration.Failed("Access denied"),
            parseRegisterResponse("""{"errorCode":"5","errorMessage":"Access denied"}"""),
        )
    }

    @Test fun `parse garbage`() {
        assertIs<OrderRegistration.Failed>(parseRegisterResponse("<html>"))
    }

    @Test fun `empty credentials skip network`() =
        runTest {
            val result = GatewayOrderRegistrar(baseUrl, "", "", http()).register(100, null)
            assertEquals(OrderRegistration.MissingCredentials, result)
            assertTrue(requests.isEmpty())
        }

    @Test fun `guest sends no clientId`() =
        runTest {
            val result = GatewayOrderRegistrar(baseUrl, "login", "secret", http()).register(157800, null)
            assertEquals(OrderRegistration.Registered("md-1"), result)
            val request = requests.single()
            assertEquals("$baseUrl/rest/register.do", request.url.toString())
            val form = request.form()
            assertEquals("157800", form["amount"])
            assertEquals("978", form["currency"])
            assertEquals("sdk://done", form["returnUrl"])
            assertEquals("login", form["userName"])
            assertEquals("secret", form["password"])
            assertFalse(form["orderNumber"].isNullOrBlank())
            assertFalse(form.contains("clientId"))
        }

    @Test fun `buyer with cards sends clientId`() =
        runTest {
            GatewayOrderRegistrar(baseUrl, "login", "secret", http()).register(157800, "c-1")
            assertEquals("c-1", requests.single().form()["clientId"])
        }

    @Test fun `orderNumber fits the 32 characters gateways allow`() =
        runTest {
            GatewayOrderRegistrar(baseUrl, "login", "secret", http()).register(100, null)
            val orderNumber = requests.single().form()["orderNumber"].orEmpty()
            assertTrue(orderNumber.length in 1..32, "orderNumber '$orderNumber' is ${orderNumber.length} chars")
        }

    @Test fun `two registrations use different orderNumber`() =
        runTest {
            val registrar = GatewayOrderRegistrar(baseUrl, "login", "secret", http())
            registrar.register(100, null)
            registrar.register(100, null)
            assertNotEquals(requests[0].form()["orderNumber"], requests[1].form()["orderNumber"])
        }

    @Test fun `network error becomes Failed`() =
        runTest {
            val failing = HttpClient(MockEngine { throw IOException("offline") })
            assertIs<OrderRegistration.Failed>(GatewayOrderRegistrar(baseUrl, "login", "secret", failing).register(100, null))
        }

    @Test fun `password is not in toString`() {
        assertFalse(GatewayOrderRegistrar(baseUrl, "login", "secret", http()).toString().contains("secret"))
    }

    @Test fun `slow gateway times out as Failed`() =
        runTest {
            val slow =
                HttpClient(
                    MockEngine {
                        kotlinx.coroutines.delay(5_000)
                        respond("""{"orderId":"late"}""", HttpStatusCode.OK)
                    },
                )
            val result =
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    GatewayOrderRegistrar(baseUrl, "login", "secret", slow, timeoutMillis = 100).register(100, null)
                }
            assertIs<OrderRegistration.Failed>(result)
        }

    // ── API V2 ───────────────────────────────────────────────────────────────

    // A session the dev gateway created, and the mdOrder of its paymentUrl.
    private val sessionId = "ps_2GRSigK2sUQKFSAZWbg2z7UuQxxjz7wqCjZqYyWRiq22aF8xxCDPua"
    private val sessionMdOrder = "0180a01a-a8cb-75b8-8f9d-1a5f00ab8a11"
    private val sessionBody = """{"id":"$sessionId","amount":1000,"status":"open","paymentUrl":"https://x?mdOrder=$sessionMdOrder"}"""

    private fun HttpRequestData.json(): JsonObject = Json.parseToJsonElement((body as TextContent).text).jsonObject

    @Test fun `api2 lives next to the payment page of the base url`() {
        assertEquals("https://gateway.example/api2", api2Url("https://gateway.example/payment"))
        assertEquals("https://gateway.example/api2", api2Url("https://gateway.example/payment/"))
        assertEquals("https://gateway.example:8443/api2", api2Url("https://gateway.example:8443/payment"))
    }

    @Test fun `parse session`() {
        assertEquals(OrderRegistration.Registered(sessionMdOrder, sessionId), parseSessionResponse(sessionBody))
    }

    @Test fun `parse api2 error`() {
        assertEquals(
            OrderRegistration.Failed("Unknown user"),
            parseSessionResponse("""{"error":{"errorCode":"unauthorized","errorMessage":"Unknown user"}}"""),
        )
    }

    @Test fun `parse api2 garbage`() {
        assertIs<OrderRegistration.Failed>(parseSessionResponse("<html>"))
        assertIs<OrderRegistration.Failed>(parseSessionResponse("""{"id":"not-a-session"}"""))
    }

    @Test fun `empty api key skips network`() =
        runTest {
            assertEquals(OrderRegistration.MissingApiKey, ApiV2SessionRegistrar(baseUrl, "", http(sessionBody)).register(100, null))
            assertTrue(requests.isEmpty())
        }

    @Test fun `api2 guest creates a session that returns to the sdk`() =
        runTest {
            val result = ApiV2SessionRegistrar(baseUrl, "key-1", http(sessionBody)).register(157800, null)
            assertEquals(OrderRegistration.Registered(sessionMdOrder, sessionId), result)
            val request = requests.single()
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("https://gateway.example/api2/sessions", request.url.toString())
            assertEquals("key-1", request.headers["X-API-KEY"])
            assertEquals(ContentType.Application.Json, (request.body as TextContent).contentType)
            val body = request.json()
            assertEquals("157800", body.getValue("amount").jsonPrimitive.content)
            assertEquals("EUR", body.getValue("currency").jsonPrimitive.content)
            assertEquals("sdk://done", body.getValue("successUrl").jsonPrimitive.content)
            assertEquals("sdk://done", body.getValue("failureUrl").jsonPrimitive.content)
            assertFalse(body.containsKey("merchantCustomerId"))
        }

    @Test fun `api2 buyer with cards sends merchantCustomerId`() =
        runTest {
            ApiV2SessionRegistrar(baseUrl, "key-1", http(sessionBody)).register(100, "c-1")
            val body = requests.single().json()
            assertEquals("c-1", body.getValue("merchantCustomerId").jsonPrimitive.content)
        }

    @Test fun `api2 network error becomes Failed`() =
        runTest {
            val failing = HttpClient(MockEngine { throw IOException("offline") })
            assertIs<OrderRegistration.Failed>(ApiV2SessionRegistrar(baseUrl, "key-1", failing).register(100, null))
        }

    @Test fun `api key is not in toString`() {
        assertFalse(ApiV2SessionRegistrar(baseUrl, "key-1", http()).toString().contains("key-1"))
    }

    // ── AFT transfers (registerP2P) ──────────────────────────────────────────

    @Test fun `an AFT order is registered as a one-leg transfer that returns to the sdk`() =
        runTest {
            val response = """{"errorCode":0,"errorMessage":"Successful","orderId":"md-aft","formUrl":"https://x"}"""
            val result =
                GatewayOrderRegistrar(baseUrl, "login", "secret", http(response), orderType = OrderType.AFT).register(100, "c-1")

            assertEquals(OrderRegistration.Registered("md-aft"), result)
            val request = requests.single()
            assertEquals("$baseUrl/rest/api/p2p/registerP2P.do", request.url.toString())
            val body = request.json()
            assertEquals("login", body.getValue("username").jsonPrimitive.content)
            assertEquals("secret", body.getValue("password").jsonPrimitive.content)
            assertEquals("100", body.getValue("amount").jsonPrimitive.content)
            assertEquals("978", body.getValue("currency").jsonPrimitive.content)
            assertEquals("sdk://done", body.getValue("returnUrl").jsonPrimitive.content)
            assertEquals("A", body.getValue("transactionTypeIndicator").jsonPrimitive.content)
            assertEquals("""{"feature":["WITHOUT_TO_CARD"]}""", body.getValue("features").toString())
            assertEquals("c-1", body.getValue("clientId").jsonPrimitive.content)
            assertFalse(
                body
                    .getValue("orderNumber")
                    .jsonPrimitive.content
                    .isBlank(),
            )
        }

    @Test fun `a refused AFT registration gives the gateway's message`() {
        assertEquals(
            OrderRegistration.Failed("Features are invalid"),
            parseRegisterResponse("""{"errorCode":14,"errorMessage":"Features are invalid"}"""),
        )
    }

    @Test fun `a payment order stays on register do`() =
        runTest {
            GatewayOrderRegistrar(baseUrl, "login", "secret", http(), orderType = OrderType.PURCHASE).register(100, null)
            assertEquals("$baseUrl/rest/register.do", requests.single().url.toString())
        }

    @Test fun `the factory passes the order type to API V1 only`() {
        val v1 = merchantRegistrar(ApiVersion.V1, baseUrl, "l", "p", "", http(), OrderType.AFT)
        assertEquals(OrderType.AFT, (v1 as GatewayOrderRegistrar).orderType)
        assertIs<ApiV2SessionRegistrar>(merchantRegistrar(ApiVersion.V2, baseUrl, "l", "p", "k", http(), OrderType.AFT))
    }

    @Test fun `an AFT order carries the prefill the app passes`() =
        runTest {
            GatewayOrderRegistrar(baseUrl, "login", "secret", http(), orderType = OrderType.AFT, prefill = NorthwindProfile.prefill)
                .register(10000, null)
            val body = requests.single().json()
            assertEquals("alex.kim@example.com", body.getValue("email").jsonPrimitive.content)
            val payer = body.getValue("billingPayerData").jsonObject
            assertEquals("Alex", payer.getValue("payerFirstName").jsonPrimitive.content)
            assertEquals("Kim", payer.getValue("payerLastName").jsonPrimitive.content)
            assertEquals("19900115", payer.getValue("payerBirthday").jsonPrimitive.content)
            assertEquals("CY", payer.getValue("billingCountry").jsonPrimitive.content)
            val recipient = body.getValue("billingRecipientData").jsonObject
            assertEquals("NW00007781", recipient.getValue("recipientAccount").jsonPrimitive.content)
            assertEquals("100", recipient.getValue("recipientAccountNumberType").jsonPrimitive.content)
            assertEquals("A", body.getValue("transactionTypeIndicator").jsonPrimitive.content)
            assertEquals("""{"feature":["WITHOUT_TO_CARD"]}""", body.getValue("features").toString())
        }

    @Test fun `an AFT order without prefill sends no payer data`() =
        runTest {
            GatewayOrderRegistrar(baseUrl, "login", "secret", http(), orderType = OrderType.AFT).register(100, null)
            val body = requests.single().json()
            assertFalse("email" in body)
            assertFalse("billingPayerData" in body)
            assertFalse("billingRecipientData" in body)
        }

    @Test fun `a payment order ignores the prefill`() =
        runTest {
            GatewayOrderRegistrar(baseUrl, "login", "secret", http(), prefill = NorthwindProfile.prefill).register(100, null)
            val form = requests.single().form()
            assertEquals("$baseUrl/rest/register.do", requests.single().url.toString())
            assertFalse(form.names().any { it == "email" || it.startsWith("billing") || it.startsWith("payer") })
        }

    @Test fun `the factory passes the prefill to API V1`() {
        val v1 = merchantRegistrar(ApiVersion.V1, baseUrl, "l", "p", "", http(), OrderType.AFT, NorthwindProfile.prefill)
        assertEquals(NorthwindProfile.prefill, (v1 as GatewayOrderRegistrar).prefill)
    }
}
