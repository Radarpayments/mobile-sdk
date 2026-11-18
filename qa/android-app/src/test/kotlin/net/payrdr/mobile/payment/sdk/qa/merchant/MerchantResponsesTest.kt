package net.payrdr.mobile.payment.sdk.qa.merchant

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.ktor.http.Parameters
import kotlin.test.Test

class MerchantResponsesTest {
    private val none = GatewaySecrets.of(V1.copy(password = ""))

    @Test
    fun `the api2 url keeps the scheme, the host and a non default port`() {
        api2Url("https://dev.bpcbt.com/payment") shouldBe "https://dev.bpcbt.com/api2"
        api2Url("  https://dev.bpcbt.com:443/payment/  ") shouldBe "https://dev.bpcbt.com/api2"
        api2Url("http://dev.bpcbt.com:8080/payment") shouldBe "http://dev.bpcbt.com:8080/api2"
    }

    @Test
    fun `a session id decodes to its mdOrder`() {
        mdOrderOfSession("ps_3Nm8Ua5Tgxnk") shouldBe null
        mdOrderOfSession("ps_emhs") shouldBe null
        mdOrderOfSession("zzz") shouldBe null
        mdOrderOfSession("ps_0OIl") shouldBe null
        mdOrderOfSession("ps_") shouldBe null
    }

    @Test
    fun `a session id of ps colon and an order decodes`() {
        // Base58 of "ps:a" (0x70 0x73 0x3a 0x61).
        mdOrderOfSession("ps_3siKPN") shouldBe "a"
    }

    @Test
    fun `an order number is qa and twenty hex digits, new each time`() {
        val first = newOrderNumber()

        first shouldMatch Regex("qa-[0-9a-f]{20}")
        (newOrderNumber() == first) shouldBe false
    }

    @Test
    fun `a register answer without an order id or an error fails with the body`() {
        registerAnswer(200, """{"formUrl":"x"}""", none) shouldBe Registration.Failed(200, """HTTP 200: {"formUrl":"x"}""")
        registerAnswer(200, """{"orderId":"  "}""", none) shouldBe Registration.Failed(200, """HTTP 200: {"orderId":"  "}""")
        registerAnswer(200, """{"errorCode":"0"}""", none) shouldBe Registration.Failed(200, """HTTP 200: {"errorCode":"0"}""")
        registerAnswer(200, "[1]", none) shouldBe Registration.Failed(200, "HTTP 200: [1]")
        registerAnswer(200, "not json", none) shouldBe Registration.Failed(200, "HTTP 200: not json")
        registerAnswer(200, "   ", none) shouldBe Registration.Failed(200, "HTTP 200: (empty body)")
    }

    @Test
    fun `a register error without a message names its code`() {
        registerAnswer(200, """{"errorCode":5}""", none) shouldBe Registration.Failed(200, "Gateway error 5")
    }

    @Test
    fun `an order id under a failing http status is not a registration`() {
        registerAnswer(500, """{"orderId":"abc"}""", none) shouldBe Registration.Failed(500, """HTTP 500: {"orderId":"abc"}""")
    }

    @Test
    fun `a session answer without an id fails`() {
        sessionAnswer(201, "{}", none) shouldBe Registration.Failed(201, "HTTP 201: {}")
        sessionAnswer(400, """{"error":{"errorCode":"7"}}""", none) shouldBe Registration.Failed(400, "Gateway error 7")
    }

    @Test
    fun `a status answer reads numbers given as text`() {
        statusAnswer(200, """{"orderStatus":"2","actionCode":"-2007","actionCodeDescription":null}""", none) shouldBe
            OrderStatus.Known(2, -2007, null)
    }

    @Test
    fun `a status answer with error code zero is known`() {
        statusAnswer(200, """{"errorCode":"0","orderStatus":0}""", none) shouldBe OrderStatus.Known(0, null, null)
    }

    @Test
    fun `secrets are masked in every form they travel in`() {
        val secrets = GatewaySecrets.of(V1.copy(password = "a+b c&d\"e"))

        secrets.mask("raw a+b c&d\"e form a%2Bb+c%26d%22e url a%2Bb%20c%26d%22e json a+b c&d\\\"e html a+b c&amp;d&quot;e") shouldBe
            "raw *** form *** url *** json *** html ***"
    }

    @Test
    fun `secrets are masked in the escapes other serializers use`() {
        val equals = GatewaySecrets.of(V1.copy(password = "p=ss"))
        val mixed = GatewaySecrets.of(V1.copy(password = "a/b+c<d"))

        equals.mask("gson p\\u003dss upper p\\u003Dss url p%3dss") shouldBe "gson *** upper *** url ***"
        mixed.mask("slash a\\/b+c<d html-safe a\\/b+c\\u003cd plain a/b+c\\u003Cd") shouldBe "slash *** html-safe *** plain ***"
        mixed.mask("lower a%2fb%2bc%3cd upper a%2Fb%2Bc%3Cd") shouldBe "lower *** upper ***"
    }

    @Test
    fun `secrets are masked in any mix of spellings`() {
        val secrets = GatewaySecrets.of(V1.copy(password = "p=s s'é😀"))

        secrets.mask("p%3Ds+s'%C3%A9😀") shouldBe "***"
        secrets.mask("p%3ds%20s&#39;é%F0%9F%98%80 tail") shouldBe "*** tail"
        secrets.mask("p&#x3D;s s&#39;\\u00e9😀") shouldBe "***"
        secrets.mask("p=s s&apos;\\u00E9\\uD83D\\ude00") shouldBe "***"
        secrets.mask("p\\u003ds s'\\u00e9😀 \\u0070=s s'é😀") shouldBe "*** ***"
        secrets.mask("p=s s'e😀") shouldBe "p=s s'e😀"
    }

    @Test
    fun `the cut counts utf-16 units and does not split a pair`() {
        none.bounded("ab😀cd", 4, "…") shouldBe "ab😀…"
        none.bounded("ab😀cd", 3, "…") shouldBe "ab…"
        none.bounded("ab😀", 4, "…") shouldBe "ab😀"
    }

    @Test
    fun `an answer beyond the document limits is not read`() {
        val deep = """{"orderId":"abc","orderStatus":2,"x":""" + "[".repeat(40) + "]".repeat(40) + "}"
        val large = """{"orderId":"abc","id":"abc","orderStatus":2,"padding":"""" + "p".repeat(70_000) + "\"}"

        registerAnswer(200, deep, none) shouldBe Registration.Failed(200, "HTTP 200: $deep")
        registerAnswer(200, large, none) shouldBe Registration.Failed(200, "HTTP 200: " + large.take(300))
        sessionAnswer(200, large, none) shouldBe Registration.Failed(200, "HTTP 200: " + large.take(300))
        statusAnswer(200, deep, none) shouldBe OrderStatus.Failed(200, "HTTP 200: $deep")
    }

    @Test
    fun `masking happens before the cut`() {
        val secrets = GatewaySecrets.of(V1)

        val text = secrets.bounded("x".repeat(295) + "password-1" + "y".repeat(10), 300)

        text shouldBe "x".repeat(295) + "***" + "yy"
        text shouldNotContain "pass"
    }

    @Test
    fun `no secrets leave the text as it is`() {
        none.mask("password-1") shouldBe "password-1"
    }

    @Test
    fun `the request is printed without its secrets`() {
        val request =
            GatewayRequest.Form(
                "https://dev.bpcbt.com/payment/rest/x",
                Parameters.build {
                    append("userName", "login-1")
                    append("password", "password-1")
                },
            )

        request.toString() shouldNotContain "password-1"
        request.printed(GatewaySecrets.of(V1)) shouldBe "→ POST https://dev.bpcbt.com/payment/rest/x userName=login-1&password=***"
    }
}
