package net.payrdr.mobile.payment.sdk.bridge

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.payrdr.mobile.payment.sdk.payment.exception.SdkAlreadyPaidException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNetworkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import kotlin.test.Test

class CheckoutResultJsonTest {
    private fun json(result: CheckoutResult): JsonObject = Json.parseToJsonElement(CheckoutResultJson.encode(result)).jsonObject

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test fun `a paid order is completed`() {
        json(CheckoutResult.Completed("md-1")) shouldBe json("""{"status":"completed","orderId":"md-1"}""")
    }

    @Test fun `a closed sheet is canceled`() {
        json(CheckoutResult.Canceled("md-1")) shouldBe json("""{"status":"canceled","orderId":"md-1"}""")
    }

    @Test fun `an unknown outcome carries the last error only when there was one`() {
        json(CheckoutResult.Unknown("md-1")) shouldBe json("""{"status":"unknown","orderId":"md-1"}""")
        json(CheckoutResult.Unknown("md-1", SdkNetworkException("offline"))) shouldBe
            json("""{"status":"unknown","orderId":"md-1","error":{"code":"network","message":"offline"}}""")
    }

    @Test fun `a decline carries the bank's text and code`() {
        json(CheckoutResult.Failed("md-1", SdkDeclinedException(declineCode = "116", message = "Insufficient funds"))) shouldBe
            json("""{"status":"failed","orderId":"md-1","error":{"code":"declined","message":"Insufficient funds","declineCode":"116"}}""")
    }

    @Test fun `a gateway error carries its http status and code`() {
        json(CheckoutResult.Failed("md-1", SdkPaymentApiException(httpStatus = 500, errorCode = "5", message = "System error"))) shouldBe
            json(
                """{"status":"failed","orderId":"md-1",
                   "error":{"code":"api","message":"System error","httpStatus":500,"gatewayErrorCode":"5"}}""",
            )
    }

    @Test fun `every SDK error has a stable code`() {
        val codes =
            listOf(
                SdkAlreadyPaidException("p-1") to "already_paid",
                SdkSessionExpiredException("md-1") to "session_expired",
                SdkException("something") to "unknown",
            ).map { (error, _) -> json(CheckoutResult.Failed("md-1", error))["error"]!!.jsonObject["code"].toString().trim('"') }

        codes shouldBe listOf("already_paid", "session_expired", "unknown")
    }

    @Test fun `a request the bridge could not read fails with its reason`() {
        json(CheckoutResultJson.invalidRequest("no mdOrder")) shouldBe
            json("""{"status":"failed","orderId":"","error":{"code":"invalid_request","message":"no mdOrder"}}""")
    }
}
