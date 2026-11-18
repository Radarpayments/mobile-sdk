package net.payrdr.mobile.payment.sdk.qa.report

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.launcher.ResultType
import net.payrdr.mobile.payment.sdk.qa.launcher.RunOutcome
import net.payrdr.mobile.payment.sdk.qa.merchant.FakeGateway
import net.payrdr.mobile.payment.sdk.qa.merchant.OrderStatus
import net.payrdr.mobile.payment.sdk.qa.merchant.V1
import net.payrdr.mobile.payment.sdk.qa.merchant.V2
import net.payrdr.mobile.payment.sdk.qa.merchant.ok
import java.util.TimeZone
import kotlin.test.Test

class RunReportTest {
    private val app = AppInfo("1.0.0-beta.1 (7)", "4.0.0-beta.1", "Google Pixel 8", "Android 15 (API 35)")
    private val config = QaConfig(profile = "v1")
    private val utc = TimeZone.getTimeZone("UTC")

    private fun completed(status: OrderStatus = OrderStatus.Known(2, 0, "Approved")) =
        RunOutcome(ResultType.COMPLETED, "abc", null, null, null, status)

    @Test
    fun `the report names the versions the device the profile and the result`() {
        val report = runReport(app, config, V1, completed(), emptyList(), utc)

        report shouldContain "== Versions ==\nQA app: 1.0.0-beta.1 (7)\nSDK: 4.0.0-beta.1"
        report shouldContain "== Device ==\nGoogle Pixel 8\nAndroid 15 (API 35)"
        report shouldContain "== Profile ==\nName: v1\nGateway URL: https://dev.bpcbt.com/payment\nAPI: v1"
        report shouldContain "== Result ==\nqa_result_type: completed\nmdOrder: abc\nSession id: -"
        report shouldContain "== Order status ==\norderStatus=2, actionCode=0, Approved"
    }

    @Test
    fun `the sections come in the contract's order`() {
        val titles =
            runReport(app, config, V1, completed(), emptyList(), utc)
                .lines()
                .filter { it.startsWith("== ") }

        titles shouldBe
            listOf(
                "== Versions ==",
                "== Device ==",
                "== Profile ==",
                "== Configuration ==",
                "== Result ==",
                "== Order status ==",
                "== Journal ==",
            )
    }

    @Test
    fun `the report prints the config as json`() {
        val report = runReport(app, config, V1, completed(), emptyList(), utc)

        report shouldContain "== Configuration ==\n${QaConfigJson.encode(config)}\n"
    }

    @Test
    fun `the report prints the journal lines`() {
        val entries = listOf(JournalEntry(3_723_004, LogLevel.INFO, "merchant", "← 200 {}"))

        val report = runReport(app, config, V1, completed(), entries, utc)

        report shouldContain "== Journal ==\n01:02:03.004 INFO merchant: ← 200 {}"
    }

    @Test
    fun `the report of a failed run carries the error and the http status`() {
        val outcome = RunOutcome(ResultType.REGISTRATION_FAILED, null, null, "Access denied", 200, OrderStatus.NotQueried)

        val report = runReport(app, config, V1, outcome, emptyList(), utc)

        report shouldContain "qa_result_type: registration_failed"
        report shouldContain "Error: Access denied"
        report shouldContain "HTTP status: 200"
        report shouldContain "== Order status ==\nnot queried: the checkout did not run"
    }

    @Test
    fun `the report says why a status was not asked`() {
        runReport(app, config, V2, completed(OrderStatus.NotQueried), emptyList(), utc) shouldContain
            "not queried: API v2 has no status query"
        runReport(app, config, V1.copy(password = ""), completed(OrderStatus.NotQueried), emptyList(), utc) shouldContain
            "not queried: the profile has no login and password"
        val nameless = RunOutcome(ResultType.CANCELED, null, null, null, null, OrderStatus.NotQueried)
        runReport(app, config, V1, nameless, emptyList(), utc) shouldContain
            "not queried: the checkout named no order"
    }

    @Test
    fun `a failed status query is printed with its error`() {
        runReport(app, config, V1, completed(OrderStatus.Failed(200, "Order not found")), emptyList(), utc) shouldContain
            "== Order status ==\nerror: Order not found"
    }

    @Test
    fun `the report of a real exchange holds no secret`() =
        runTest {
            // Each call's gateway quotes the secret it was sent, as a stand's error page may.
            val gateway =
                FakeGateway {
                    val quoted = if (it.url.encodedPath.endsWith("/sessions")) "key-1" else "password-1"
                    ok("""{"orderId":"abc","id":"session-1","echo":"$quoted"}""")
                }
            val merchant = gateway.merchant()
            merchant.register(config.order, V1)
            merchant.register(config.order, V2)

            val report = runReport(app, config, V1, completed(), gateway.journal.entries.value, utc)

            report shouldNotContain "password-1"
            report shouldNotContain "key-1"
            report shouldContain "dev.bpcbt.com"
            report shouldContain "/rest/register.do"
        }
}
