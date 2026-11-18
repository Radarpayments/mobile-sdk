package net.payrdr.mobile.payment.sdk.qa.merchant

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.OrderType
import java.io.IOException
import java.math.BigInteger
import kotlin.test.Test

class MerchantRegisterTest {
    private fun extra(json: String): JsonObject = Json.parseToJsonElement(json).jsonObject

    @Test
    fun `a payment order is a register_do form`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(), V1)

            val sent = gateway.single()
            sent.method shouldBe "POST"
            sent.url shouldBe "https://dev.bpcbt.com/payment/rest/register.do"
            sent.contentType?.withoutParameters() shouldBe ContentType.Application.FormUrlEncoded
            val form = sent.form()
            form.names().toList() shouldBe listOf("userName", "password", "amount", "currency", "orderNumber", "returnUrl")
            form["userName"] shouldBe "login-1"
            form["password"] shouldBe "password-1"
            form["amount"] shouldBe "10000"
            form["currency"] shouldBe "978"
            form["orderNumber"] shouldBe "qa-1"
            form["returnUrl"] shouldBe "sdk://done"
        }

    @Test
    fun `a trailing slash of the gateway url is not doubled`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(), V1.copy(gatewayUrl = "https://dev.bpcbt.com/payment/"))

            gateway.single().url shouldBe "https://dev.bpcbt.com/payment/rest/register.do"
        }

    @Test
    fun `the client id and the features join the form`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(clientId = "c1", features = listOf("VERIFY", "FORCE_CREATE_BINDING")), V1)

            val form = gateway.single().form()
            form["clientId"] shouldBe "c1"
            form["features"] shouldBe "VERIFY,FORCE_CREATE_BINDING"
        }

    @Test
    fun `the currency goes as its numeric code`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(currency = "USD"), V1)

            gateway.single().form()["currency"] shouldBe "840"
        }

    @Test
    fun `an unknown currency fails before anything is sent`() =
        runTest {
            val gateway = FakeGateway()

            val result = gateway.merchant().register(OrderConfig(currency = "XXZ"), V1)

            result shouldBe Registration.Failed(null, "Unknown currency XXZ")
            gateway.requests.size shouldBe 0
        }

    @Test
    fun `extra replaces a form parameter and adds its own`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(extra = extra("""{"returnUrl":"app://x","jsonParams":{"a":1}}""")), V1)

            val form = gateway.single().form()
            form.getAll("returnUrl") shouldBe listOf("app://x")
            form["jsonParams"] shouldBe """{"a":1}"""
        }

    @Test
    fun `scalar extra values go into the form as their text`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(extra = extra("""{"n":5,"b":true,"s":"x y","list":[1,"a"]}""")), V1)

            val form = gateway.single().form()
            form["n"] shouldBe "5"
            form["b"] shouldBe "true"
            form["s"] shouldBe "x y"
            form["list"] shouldBe """[1,"a"]"""
        }

    @Test
    fun `an aft order is a registerP2P json with WITHOUT_TO_CARD first`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(type = OrderType.AFT, features = listOf("VERIFY", "WITHOUT_TO_CARD")), V1)

            val sent = gateway.single()
            sent.url shouldBe "https://dev.bpcbt.com/payment/rest/api/p2p/registerP2P.do"
            sent.contentType?.withoutParameters() shouldBe ContentType.Application.Json
            val body = sent.json()
            body["username"] shouldBe JsonPrimitive("login-1")
            body["password"] shouldBe JsonPrimitive("password-1")
            body["amount"] shouldBe JsonPrimitive(10_000)
            body["currency"] shouldBe JsonPrimitive("978")
            body["orderNumber"] shouldBe JsonPrimitive("qa-1")
            body["returnUrl"] shouldBe JsonPrimitive("sdk://done")
            body["transactionTypeIndicator"] shouldBe JsonPrimitive("A")
            body["features"] shouldBe extra("""{"feature":["WITHOUT_TO_CARD","VERIFY"]}""")
            body.containsKey("clientId") shouldBe false
        }

    @Test
    fun `an aft order carries the client id`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(type = OrderType.AFT, clientId = "c1"), V1)

            val body = gateway.single().json()
            body["clientId"] shouldBe JsonPrimitive("c1")
            body["features"] shouldBe extra("""{"feature":["WITHOUT_TO_CARD"]}""")
        }

    @Test
    fun `extra replaces a whole json value`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(type = OrderType.AFT, extra = extra("""{"features":{"feature":["X"]}}""")), V1)

            gateway.single().json()["features"] shouldBe extra("""{"feature":["X"]}""")
        }

    @Test
    fun `a v2 profile opens a session`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(clientId = "c1"), V2)

            val sent = gateway.single()
            sent.url shouldBe "https://dev.bpcbt.com/api2/sessions"
            sent.headers["X-API-KEY"] shouldBe "key-1"
            sent.contentType?.withoutParameters() shouldBe ContentType.Application.Json
            val body = sent.json()
            body["amount"] shouldBe JsonPrimitive(10_000)
            body["currency"] shouldBe JsonPrimitive("EUR")
            body["successUrl"] shouldBe JsonPrimitive("sdk://done")
            body["failureUrl"] shouldBe JsonPrimitive("sdk://done")
            body["merchantCustomerId"] shouldBe JsonPrimitive("c1")
            body.containsKey("features") shouldBe false
        }

    @Test
    fun `a session carries the features as an array`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(features = listOf("VERIFY")), V2)

            val body = gateway.single().json()
            body["features"] shouldBe JsonArray(listOf(JsonPrimitive("VERIFY")))
            body.containsKey("merchantCustomerId") shouldBe false
        }

    @Test
    fun `an aft order on a v2 profile fails before anything is sent`() =
        runTest {
            val gateway = FakeGateway()

            val result = gateway.merchant().register(OrderConfig(type = OrderType.AFT), V2)

            result.shouldBeInstanceOf<Registration.Failed>().httpStatus shouldBe null
            gateway.requests.size shouldBe 0
        }

    @Test
    fun `a registered order returns its mdOrder`() =
        runTest {
            val gateway = FakeGateway { ok("""{"orderId":"abc","formUrl":"https://dev.bpcbt.com/payment/x"}""") }

            gateway.merchant().register(OrderConfig(), V1) shouldBe Registration.Registered("abc", null)
        }

    @Test
    fun `a session returns its id and the decoded mdOrder`() =
        runTest {
            val mdOrder = "0f0e1d2c-3b4a-5968-7766-554433221100"
            val id = sessionIdOf(mdOrder)
            val gateway = FakeGateway { ok("""{"id":"$id"}""") }

            gateway.merchant().register(OrderConfig(), V2) shouldBe Registration.Registered(mdOrder, id)
        }

    @Test
    fun `a session id that does not decode still registers`() =
        runTest {
            val gateway = FakeGateway { ok("""{"id":"zzz"}""") }

            gateway.merchant().register(OrderConfig(), V2) shouldBe Registration.Registered(null, "zzz")
        }

    @Test
    fun `a session error carries the gateway's message`() =
        runTest {
            val gateway = FakeGateway { reply(HttpStatusCode.BadRequest, """{"error":{"errorCode":"7","errorMessage":"Bad amount"}}""") }

            gateway.merchant().register(OrderConfig(), V2) shouldBe Registration.Failed(400, "Bad amount")
        }

    @Test
    fun `a gateway error carries its message and the http status`() =
        runTest {
            val gateway = FakeGateway { ok("""{"errorCode":"5","errorMessage":"Access denied"}""") }

            gateway.merchant().register(OrderConfig(), V1) shouldBe Registration.Failed(200, "Access denied")
        }

    @Test
    fun `an html error page fails with the status and the start of the body`() =
        runTest {
            val page = "<html><body>" + "x".repeat(1_000) + "</body></html>"
            val gateway = FakeGateway { reply(HttpStatusCode.BadGateway, page, ContentType.Text.Html.withCharset(Charsets.UTF_8)) }

            val failed = gateway.merchant().register(OrderConfig(), V1).shouldBeInstanceOf<Registration.Failed>()

            failed.httpStatus shouldBe 502
            failed.message shouldStartWith "HTTP 502: <html>"
            failed.message.length shouldBe "HTTP 502: ".length + 300
        }

    @Test
    fun `an empty body fails with the status`() =
        runTest {
            val gateway = FakeGateway { ok("") }

            val failed = gateway.merchant().register(OrderConfig(), V1).shouldBeInstanceOf<Registration.Failed>()

            failed.httpStatus shouldBe 200
            failed.message shouldBe "HTTP 200: (empty body)"
        }

    @Test
    fun `no answer in time fails without a status`() =
        runTest {
            val gateway =
                FakeGateway {
                    delay(10_000)
                    ok("""{"orderId":"abc"}""")
                }

            gateway.merchant(timeoutMillis = 50).register(OrderConfig(), V1) shouldBe
                Registration.Failed(null, "The gateway did not answer in 0 s")
        }

    @Test
    fun `a network failure fails without a status`() =
        runTest {
            val gateway = FakeGateway { throw IOException("Connection reset") }

            gateway.merchant().register(OrderConfig(), V1) shouldBe Registration.Failed(null, "IOException: Connection reset")
        }

    @Test
    fun `cancellation is not swallowed`() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val gateway =
                FakeGateway {
                    entered.complete(Unit)
                    awaitCancellation()
                }
            var outcome: Result<Registration>? = null

            val job = launch { outcome = runCatching { gateway.merchant().register(OrderConfig(), V1) } }
            entered.await()
            job.cancel()
            job.join()

            outcome?.exceptionOrNull().shouldBeInstanceOf<CancellationException>()
        }

    @Test
    fun `the caller's own timeout is not turned into a failure`() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val gateway =
                FakeGateway {
                    entered.complete(Unit)
                    awaitCancellation()
                }
            var outcome: Result<Registration>? = null

            // Real time: under the test scheduler the caller's timeout could fire before the request reaches the engine.
            withContext(Dispatchers.Default) {
                withTimeoutOrNull(1_000) { outcome = runCatching { gateway.merchant().register(OrderConfig(), V1) } }
            }

            entered.isCompleted shouldBe true
            outcome?.exceptionOrNull().shouldBeInstanceOf<CancellationException>()
            gateway.journalText() shouldNotContain "did not answer"
        }

    @Test
    fun `an http stack that reports our timeout as an io error still times out`() =
        runTest {
            val gateway = FakeGateway { awaitCancellation() }
            // Ktor's engine pipeline turns the engine's own error into a cancellation, so the IOException that OkHttp
            // throws for a cancelled call is raised where it reaches the merchant directly.
            gateway.client.plugin(HttpSend).intercept { request ->
                try {
                    execute(request)
                } catch (_: CancellationException) {
                    throw IOException("Canceled")
                }
            }

            gateway.merchant(timeoutMillis = 50).register(OrderConfig(), V1) shouldBe
                Registration.Failed(null, "The gateway did not answer in 0 s")
            gateway.journalText() shouldNotContain "IOException"
        }

    @Test
    fun `a null in extra removes a form parameter`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(extra = extra("""{"returnUrl":null,"absent":null}""")), V1)

            gateway
                .single()
                .form()
                .names()
                .toList() shouldBe listOf("userName", "password", "amount", "currency", "orderNumber")
        }

    @Test
    fun `a null in extra removes a json value`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().register(OrderConfig(type = OrderType.AFT, extra = extra("""{"returnUrl":null,"absent":null}""")), V1)
            gateway.merchant().register(OrderConfig(extra = extra("""{"failureUrl":null}""")), V2)

            val (transfer, session) = gateway.requests.map { it.json() }
            transfer.keys shouldBe
                setOf("username", "password", "amount", "currency", "orderNumber", "transactionTypeIndicator", "features")
            session.keys shouldBe setOf("amount", "currency", "successUrl")
        }

    @Test
    fun `the journal shows that the api key went out, not the key`() =
        runTest {
            val gateway = FakeGateway { ok("""{"id":"zzz"}""") }

            gateway.merchant().register(OrderConfig(), V2)
            gateway.merchant().register(OrderConfig(type = OrderType.AFT), V1)

            val (session, transfer) =
                gateway.journal.entries.value
                    .filter { it.message.startsWith("→") }
                    .map { it.message }
            session shouldStartWith "→ POST https://dev.bpcbt.com/api2/sessions X-API-KEY: *** {"
            session shouldNotContain "key-1"
            transfer shouldNotContain "X-API-KEY"
        }

    @Test
    fun `the journal shows the exchange without the password and the api key`() =
        runTest {
            val gateway = FakeGateway { ok("""{"orderId":"abc"}""") }

            gateway.merchant().register(OrderConfig(), V1)
            gateway.merchant().register(OrderConfig(type = OrderType.AFT), V1)
            gateway.merchant().register(OrderConfig(), V2)

            val journal = gateway.journalText()
            journal shouldContain "→ POST https://dev.bpcbt.com/payment/rest/register.do"
            journal shouldContain "login-1"
            journal shouldContain "← 200 {\"orderId\":\"abc\"}"
            journal shouldContain "***"
            journal shouldNotContain "password-1"
            journal shouldNotContain "key-1"
            gateway.journal.entries.value
                .map { it.tag }
                .toSet() shouldBe setOf("merchant")
        }

    @Test
    fun `a reply that echoes the secrets keeps them out of the journal and the message`() =
        runTest {
            val profile = V1.copy(password = "pa ss&\"zq\"-1")
            val echo =
                """raw=pa ss&"zq"-1 form=pa+ss%26%22zq%22-1 url=pa%20ss%26%22zq%22-1 """ +
                    """json=pa ss&\"zq\"-1 html=pa ss&amp;&quot;zq&quot;-1"""
            val gateway = FakeGateway { reply(HttpStatusCode.InternalServerError, "<pre>userName=login-1 $echo</pre>") }

            val failed = gateway.merchant().register(OrderConfig(), profile).shouldBeInstanceOf<Registration.Failed>()

            failed.message shouldBe "HTTP 500: <pre>userName=login-1 raw=*** form=*** url=*** json=*** html=***</pre>"
            gateway.journalText() shouldNotContain "zq"
        }

    @Test
    fun `a secret on the edge of the cut is not left half visible`() =
        runTest {
            val body = "x".repeat(295) + "password-1" + "y".repeat(100)
            val gateway = FakeGateway { reply(HttpStatusCode.InternalServerError, body) }

            val failed = gateway.merchant().register(OrderConfig(), V1).shouldBeInstanceOf<Registration.Failed>()

            failed.message shouldNotContain "passw"
            failed.message shouldContain "x***"
        }

    @Test
    fun `a gateway message that quotes the api key is masked`() =
        runTest {
            val gateway = FakeGateway { reply(HttpStatusCode.Unauthorized, """{"error":{"errorMessage":"Unknown key key-1"}}""") }

            gateway.merchant().register(OrderConfig(), V2) shouldBe Registration.Failed(401, "Unknown key ***")
            gateway.journalText() shouldNotContain "key-1"
        }

    private fun sessionIdOf(mdOrder: String): String {
        val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
        val bytes = "ps:$mdOrder".toByteArray(Charsets.UTF_8)
        var number = BigInteger(1, bytes)
        val digits = StringBuilder()
        val base = BigInteger.valueOf(58)
        while (number > BigInteger.ZERO) {
            val (quotient, remainder) = number.divideAndRemainder(base)
            digits.append(alphabet[remainder.toInt()])
            number = quotient
        }
        repeat(bytes.takeWhile { it.toInt() == 0 }.size) { digits.append('1') }
        return "ps_" + digits.reverse()
    }
}
