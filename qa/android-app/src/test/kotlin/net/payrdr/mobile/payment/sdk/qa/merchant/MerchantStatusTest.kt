package net.payrdr.mobile.payment.sdk.qa.merchant

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MerchantStatusTest {
    @Test
    fun `the status is asked with the order id`() =
        runTest {
            val gateway =
                FakeGateway {
                    ok("""{"errorCode":"0","orderStatus":2,"actionCode":0,"actionCodeDescription":"Approved","amount":10000}""")
                }

            val status = gateway.merchant().status("abc", V1)

            status shouldBe OrderStatus.Known(2, 0, "Approved")
            val sent = gateway.single()
            sent.method shouldBe "POST"
            sent.url shouldBe "https://dev.bpcbt.com/payment/rest/getOrderStatusExtended.do"
            val form = sent.form()
            form.names().toList() shouldBe listOf("userName", "password", "orderId")
            form["userName"] shouldBe "login-1"
            form["password"] shouldBe "password-1"
            form["orderId"] shouldBe "abc"
        }

    @Test
    fun `a v2 profile is not queried`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().status("abc", V2) shouldBe OrderStatus.NotQueried
            gateway.requests.size shouldBe 0
        }

    @Test
    fun `a profile without credentials is not queried`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().status("abc", V1.copy(password = "")) shouldBe OrderStatus.NotQueried
            gateway.merchant().status("abc", V1.copy(login = " ")) shouldBe OrderStatus.NotQueried
            gateway.requests.size shouldBe 0
        }

    @Test
    fun `no order id is not queried`() =
        runTest {
            val gateway = FakeGateway()

            gateway.merchant().status(null, V1) shouldBe OrderStatus.NotQueried
            gateway.merchant().status("", V1) shouldBe OrderStatus.NotQueried
            gateway.requests.size shouldBe 0
        }

    @Test
    fun `a status error carries the gateway's message`() =
        runTest {
            val gateway = FakeGateway { ok("""{"errorCode":"6","errorMessage":"Order not found"}""") }

            gateway.merchant().status("abc", V1) shouldBe OrderStatus.Failed(200, "Order not found")
        }

    @Test
    fun `a status error without a message names its code`() =
        runTest {
            val gateway = FakeGateway { ok("""{"errorCode":"6"}""") }

            gateway.merchant().status("abc", V1) shouldBe OrderStatus.Failed(200, "Gateway error 6")
        }

    @Test
    fun `a status answer without any field fails with the body`() =
        runTest {
            val gateway = FakeGateway { reply(HttpStatusCode.ServiceUnavailable, "busy") }

            gateway.merchant().status("abc", V1) shouldBe OrderStatus.Failed(503, "HTTP 503: busy")
        }

    @Test
    fun `the status exchange is journaled without the password`() =
        runTest {
            val gateway = FakeGateway { ok("""{"orderStatus":2}""") }

            gateway.merchant().status("abc", V1) shouldBe OrderStatus.Known(2, null, null)

            val journal = gateway.journalText()
            journal shouldContain "getOrderStatusExtended.do"
            journal shouldContain "orderId=abc"
            journal shouldNotContain "password-1"
        }
}
