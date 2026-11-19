package net.payrdr.mobile.payment.sdk.qa.merchant

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.okhttp.OkHttpConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MerchantHttpTest {
    @Test
    fun `the engine has no timeout of its own, the merchant's is the only one`() {
        val client = merchantHttpClient()

        val okHttp =
            client.engine.config
                .shouldBeInstanceOf<OkHttpConfig>()
                .preconfigured!!

        okHttp.connectTimeoutMillis shouldBe 0
        okHttp.readTimeoutMillis shouldBe 0
        okHttp.writeTimeoutMillis shouldBe 0
        okHttp.callTimeoutMillis shouldBe 0
        client.close()
    }

    @Test
    fun `the client is built on the first request, not on the caller's thread`() =
        runTest {
            val gateway = FakeGateway()
            var builtOn: Thread? = null
            val client =
                lazy {
                    builtOn = Thread.currentThread()
                    gateway.client
                }
            val merchant = Merchant(client, gateway.journal) { "qa-1" }

            client.isInitialized() shouldBe false
            merchant.status("abc", V1)

            gateway.requests.size shouldBe 1
            builtOn shouldNotBe null
            builtOn shouldNotBe Thread.currentThread()
        }
}
