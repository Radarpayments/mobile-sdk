package net.payrdr.mobile.payment.sdk.qa.report

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.launcher.ResultType
import net.payrdr.mobile.payment.sdk.qa.launcher.RunOutcome
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState
import net.payrdr.mobile.payment.sdk.qa.merchant.FakeGateway
import net.payrdr.mobile.payment.sdk.qa.merchant.OrderStatus
import net.payrdr.mobile.payment.sdk.qa.merchant.V1
import net.payrdr.mobile.payment.sdk.qa.merchant.V2
import net.payrdr.mobile.payment.sdk.qa.merchant.ok
import java.io.File
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test

class RunReportTest {
    private val app = AppInfo("1.0.0-beta.1 (7)", "4.0.0-beta.1", "Google Pixel 8", "Android 15 (API 35)")
    private val config = QaConfig(profile = "v1")
    private val utc = TimeZone.getTimeZone("UTC")

    private fun reportOf(
        profile: StandProfile,
        outcome: RunOutcome,
        entries: List<JournalEntry>,
        zone: TimeZone,
    ) = runReport(app, RunState.Finished(outcome, config, profile), entries, zone)

    private fun completed(status: OrderStatus = OrderStatus.Known(2, 0, "Approved")) =
        RunOutcome(ResultType.COMPLETED, "abc", null, null, null, status)

    @Test
    fun `the report names the versions the device the profile and the result`() {
        val report = reportOf(V1, completed(), emptyList(), utc)

        report shouldContain "== Versions ==\nQA app: 1.0.0-beta.1 (7)\nSDK: 4.0.0-beta.1"
        report shouldContain "== Device ==\nGoogle Pixel 8\nAndroid 15 (API 35)"
        report shouldContain "== Profile ==\nName: v1\nGateway URL: https://dev.bpcbt.com/payment\nAPI: v1"
        report shouldContain "== Result ==\nqa_result_type: completed\nmdOrder: abc\nSession id: -"
        report shouldContain "== Order status ==\norderStatus=2, actionCode=0, Approved"
    }

    @Test
    fun `the sections come in the contract's order`() {
        val titles =
            reportOf(V1, completed(), emptyList(), utc)
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
        val report = reportOf(V1, completed(), emptyList(), utc)

        report shouldContain "== Configuration ==\n${QaConfigJson.encode(config)}\n"
    }

    @Test
    fun `the report prints the journal lines`() {
        val entries = listOf(JournalEntry(3_723_004, LogLevel.INFO, "merchant", "← 200 {}"))

        val report = reportOf(V1, completed(), entries, utc)

        report shouldContain "== Journal ==\nTime zone: UTC+00:00\n01:02:03.004 INFO merchant: ← 200 {}"
    }

    @Test
    fun `the report of a failed run carries the error and the http status`() {
        val outcome = RunOutcome(ResultType.REGISTRATION_FAILED, null, null, "Access denied", 200, OrderStatus.NotQueried)

        val report = reportOf(V1, outcome, emptyList(), utc)

        report shouldContain "qa_result_type: registration_failed"
        report shouldContain "Error: Access denied"
        report shouldContain "HTTP status: 200"
        report shouldContain "== Order status ==\nnot queried: the checkout did not run"
    }

    @Test
    fun `the report says why a status was not asked`() {
        reportOf(V2, completed(OrderStatus.NotQueried), emptyList(), utc) shouldContain
            "not queried: API v2 has no status query"
        reportOf(V1.copy(password = ""), completed(OrderStatus.NotQueried), emptyList(), utc) shouldContain
            "not queried: the profile has no login and password"
        val nameless = RunOutcome(ResultType.CANCELED, null, null, null, null, OrderStatus.NotQueried)
        reportOf(V1, nameless, emptyList(), utc) shouldContain
            "not queried: the checkout named no order"
    }

    @Test
    fun `a failed status query is printed with its error`() {
        reportOf(V1, completed(OrderStatus.Failed(200, "Order not found")), emptyList(), utc) shouldContain
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

            val report = reportOf(V1, completed(), gateway.journal.entries.value, utc)

            report shouldNotContain "password-1"
            report shouldNotContain "key-1"
            report shouldContain "dev.bpcbt.com"
            report shouldContain "/rest/register.do"
        }

    @Test
    fun `the report describes the config and the profile of its own run`() {
        val launched = QaConfig(profile = "v2", order = OrderConfig(amount = 4_200))
        val finished = RunState.Finished(completed(), launched, V2)

        val report = runReport(app, finished, emptyList(), utc)

        report shouldContain "== Configuration ==\n${QaConfigJson.encode(launched)}\n"
        report shouldContain "Name: v2"
        report shouldContain "API: v2"
    }

    @Test
    fun `the journal names its time zone`() {
        val moscow = TimeZone.getTimeZone("Europe/Moscow")
        val entries = listOf(JournalEntry(0, LogLevel.INFO, "qa", "a line"))

        runReport(app, RunState.Finished(completed(), config, V1), entries, moscow) shouldContain
            "== Journal ==\nTime zone: UTC+03:00\n03:00:00.000 INFO qa: a line"
    }

    @Test
    fun `a utc offset is printed by hand whatever the locale`() {
        val saved = Locale.getDefault()
        // Egyptian Arabic formats numbers with Arabic-Indic digits by default.
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        try {
            utcOffset(TimeZone.getTimeZone("UTC"), 0) shouldBe "UTC+00:00"
            utcOffset(TimeZone.getTimeZone("Asia/Kolkata"), 0) shouldBe "UTC+05:30"
            utcOffset(TimeZone.getTimeZone("America/St_Johns"), 0) shouldBe "UTC-03:30"
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun `the report is the readme's example to the character`() {
        val mdOrder = "8f1e4c2a-0b3d-4e5f-9a6b-7c8d9e0f1a2b"
        val finished =
            RunState.Finished(
                RunOutcome(ResultType.COMPLETED, mdOrder, null, null, null, OrderStatus.Known(2, 0, "Approved")),
                QaConfig(profile = "dev"),
                StandProfile("dev", "https://dev.bpcbt.com/payment", login = "login-1", password = "password-1"),
            )
        val entries =
            listOf(
                JournalEntry(45_296_789, LogLevel.INFO, "merchant", "→ POST $REGISTER userName=login-1&password=***"),
                JournalEntry(45_297_102, LogLevel.INFO, "merchant", "← 200 {\"orderId\":\"$mdOrder\"}"),
            )

        runReport(app, finished, entries, utc) shouldBe readmeExample()
    }

    private companion object {
        const val REGISTER = "https://dev.bpcbt.com/payment/rest/register.do"

        // The tests run in qa/android-app, so the contract is one level up, as FixtureCasesTest finds the fixtures.
        fun readmeExample(): String {
            val readme = File("../README.md").readText()
            val section = readme.indexOf("\n### Run report\n")
            check(section >= 0) { "qa/README.md has no \"### Run report\" section" }
            val start = readme.indexOf(FENCE, section)
            check(start >= 0) { "the \"Run report\" section of qa/README.md has no ```text block" }
            val end = readme.indexOf("\n```", start + FENCE.length)
            check(end >= 0) { "the ```text block of the \"Run report\" section is not closed" }
            return readme.substring(start + FENCE.length, end + 1)
        }

        const val FENCE = "```text\n"
    }
}
