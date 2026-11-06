package net.payrdr.mobile.payment.sdk.bridge

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test

class CoreTokenTest {
    private fun token(request: String): JsonObject = Json.parseToJsonElement(PayrdrBridge.coreToken(request)).jsonObject

    private val key = Json.encodeToString(JsonPrimitive.serializer(), JsonPrimitive(TEST_PUBLIC_KEY_PEM))

    private val card = """"pan":"4532896701439077","expiry":"12/28","cvc":"444","cardholder":"JOHN DOE""""

    @Test fun `a new card for an order gives a token`() {
        val result = token("""{"type":"card","mdOrder":"md-1",$card,"pubKey":$key}""")

        result["token"] shouldNotBe null
        result["errors"] shouldBe null
    }

    @Test fun `a new card without an order gives a token`() {
        token("""{"type":"cardInstant",$card,"pubKey":$key}""")["token"] shouldNotBe null
    }

    @Test fun `a saved card for an order and without one give tokens`() {
        token("""{"type":"binding","mdOrder":"md-1","bindingId":"b-1","cvc":"444","pubKey":$key}""")["token"] shouldNotBe null
        token("""{"type":"bindingInstant","bindingId":"b-1","pubKey":$key}""")["token"] shouldNotBe null
    }

    @Test fun `a request that does not parse never echoes the card back`() {
        val badShapes =
            listOf(
                // A number where a string goes, a field of the wrong type, broken JSON after the card data.
                """{"type":"card","pan":4532896701439077,"expiry":"12/28","cvc":"444","pubKey":$key}""",
                """{"type":"card","pan":"4532896701439077","expiry":"12/28","cvc":{"v":"444"},"pubKey":$key}""",
                """{"type":"card","pan":"4532896701439077","expiry":"12/28","cvc":"444","pubKey":$key""",
            )
        for (request in badShapes) {
            val response = PayrdrBridge.coreToken(request)

            token(request)["error"]!!.jsonObject["code"]!!.jsonPrimitive.content shouldBe "invalid_request"
            response shouldNotContain "4532896701439077"
            response shouldNotContain "444"
        }
    }

    @Test fun `a request that does not parse names where it broke`() {
        val response = token("""{"type":"card","pan":4532896701439077,"pubKey":$key}""")

        response["error"]!!.jsonObject["message"]!!.jsonPrimitive.content shouldContain "$.pan"
    }

    @Test fun `missing fields come back as sdk-core codes keyed by field`() {
        val result = token("""{"type":"card","mdOrder":"md-1","pan":"","expiry":"12/28","cvc":"444","pubKey":$key}""")

        result["token"] shouldBe null
        result["errors"]!!.jsonObject["pan"]!!.jsonPrimitive.content shouldBe "required"
    }

    @Test fun `an order left empty for a card that needs one is reported instead of dropped`() {
        val result = token("""{"type":"binding","mdOrder":"","bindingId":"b-1","pubKey":$key}""")

        result["errors"]!!.jsonObject.keys shouldBe setOf("mdOrder")
    }

    @Test fun `each field a plugin's form shows comes back under its own key`() {
        fun errorKeys(request: String) = token(request)["errors"]!!.jsonObject.keys

        errorKeys("""{"type":"cardInstant","pan":"4532896701439077","expiry":"13/28","cvc":"444","pubKey":$key}""") shouldBe setOf("expiry")
        errorKeys("""{"type":"cardInstant","pan":"4532896701439077","expiry":"12/28","cvc":"4","pubKey":$key}""") shouldBe setOf("cvc")
        errorKeys(
            """{"type":"cardInstant","pan":"4532896701439077","expiry":"12/28","cvc":"444","cardholder":"J0HN","pubKey":$key}""",
        ) shouldBe setOf("cardholder")
        errorKeys("""{"type":"cardInstant",$card,"pubKey":"not a key"}""") shouldBe setOf("pubKey")
        errorKeys("""{"type":"bindingInstant","bindingId":"","pubKey":$key}""") shouldBe setOf("bindingId")
    }

    @Test fun `an unknown type is an invalid request`() {
        token("""{"type":"wallet","pubKey":$key}""")["error"]!!.jsonObject["code"]!!.jsonPrimitive.content shouldBe "invalid_request"
        token("nope")["error"]!!.jsonObject["code"]!!.jsonPrimitive.content shouldBe "invalid_request"
    }
}

// The fixture key of sdk-core's tests.
internal const val TEST_PUBLIC_KEY_PEM = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvjG7qd50WLuBmw7rUpQo
xN44zA/XUddLxYAN22fJE9GbLMwKcT0fVE//KjAUDDy3s8p4QuNuxLfUsINM4DlV
IbQZx3ZOXUaQILYCqmqqvbibEk4ulITdMS6QjEsHXdcKXpr+1ZyVG90RI7oxyQf8
YKTxqOQF7lYqicEb6ucPJwYxLA1IoPu9K5PAysLrFiEwuq+z6dNFrvVMXSDAefry
Hc/MnjXhGLc3qcfH9sJczKQ8we6I7zIDgRcIjsZ8xI8ME+X36UJpYPvrU41Xssi1
6MCObHbZpZTEjShPnfmwTIDMbjnqwitXKaC0C+vJ+DvnL5+iqsHN+HT+z8NcQXco
xQIDAQAB
-----END PUBLIC KEY-----"""
