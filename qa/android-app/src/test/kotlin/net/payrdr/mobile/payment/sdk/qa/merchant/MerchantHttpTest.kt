package net.payrdr.mobile.payment.sdk.qa.merchant

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.okhttp.OkHttpConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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

    @Test
    fun `a slow first build of the client is neither journaled as sent nor counted against the timeout`() =
        runTest {
            // Loads Ktor's classes first, so the timed exchange below is short even on a loaded machine.
            FakeGateway().merchant().status("abc", V1)
            val gateway = FakeGateway { ok("""{"orderStatus":2}""") }
            var linesWhenBuilt = -1
            val client =
                lazy {
                    linesWhenBuilt = gateway.journal.entries.value.size
                    // Real time: the merchant's timeout runs on the IO dispatcher, outside the test scheduler.
                    Thread.sleep(SLOW_BUILD_MILLIS)
                    gateway.client
                }
            val merchant = Merchant(client, gateway.journal, timeoutMillis = TIMEOUT_MILLIS) { "qa-1" }

            merchant.status("abc", V1) shouldBe OrderStatus.Known(2, null, null)
            linesWhenBuilt shouldBe 0
        }

    @Test
    fun `a caller cancelled while the client is built sends and journals nothing`() =
        runTest {
            val gateway = FakeGateway()
            val building = CountDownLatch(1)
            val release = CountDownLatch(1)
            val client =
                lazy {
                    building.countDown()
                    check(release.await(LATCH_SECONDS, TimeUnit.SECONDS))
                    gateway.client
                }
            var outcome: Result<OrderStatus>? = null

            val job =
                launch(Dispatchers.Default) {
                    outcome = runCatching { Merchant(client, gateway.journal) { "qa-1" }.status("abc", V1) }
                }
            building.await(LATCH_SECONDS, TimeUnit.SECONDS) shouldBe true
            job.cancel()
            release.countDown()
            job.join()

            outcome?.exceptionOrNull().shouldBeInstanceOf<CancellationException>()
            gateway.requests.shouldBeEmpty()
            gateway.journal.entries.value
                .shouldBeEmpty()
        }

    private companion object {
        const val TIMEOUT_MILLIS = 1_000L
        const val SLOW_BUILD_MILLIS = 1_500L
        const val LATCH_SECONDS = 5L
    }
}
