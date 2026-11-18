package net.payrdr.mobile.payment.sdk.qa.merchant

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.okhttp.OkHttpConfig
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
}
