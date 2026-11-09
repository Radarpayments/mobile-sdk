package net.payrdr.mobile.payment.sdk.payment.api.dto.response

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class WalletSuccessSerializerTest {
    private fun success(json: String): Boolean = Json.decodeFromString(WalletSuccessSerializer, json)

    @Test
    fun `the gateway's booleans are read as they are`() {
        success("true") shouldBe true
        success("false") shouldBe false
    }

    @Test
    fun `older gateways' strings are read in any case`() {
        success("\"true\"") shouldBe true
        success("\"TRUE\"") shouldBe true
        success("\"false\"") shouldBe false
    }

    @Test
    fun `anything else is not a success`() {
        success("null") shouldBe false
        success("\"yes\"") shouldBe false
        success("1") shouldBe false
        success("{}") shouldBe false
    }

    @Test
    fun `it is written back as a boolean`() {
        Json.encodeToString(WalletSuccessSerializer, true) shouldBe "true"
    }
}
