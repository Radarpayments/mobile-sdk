package net.payrdr.mobile.payment.sdk.qa.ui

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.GooglePayOptions
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.OrderType
import net.payrdr.mobile.payment.sdk.qa.config.Pin
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.SdkOptions
import net.payrdr.mobile.payment.sdk.qa.config.SslPinningOptions
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.launcher.ResultType
import net.payrdr.mobile.payment.sdk.qa.merchant.OrderStatus
import java.util.TimeZone
import kotlin.test.Test

class SummariesTest {
    private val pin = Pin("dev.bpcbt.com", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")

    @Test
    fun `the order row names the source and what it starts from`() {
        orderSummary(OrderConfig()) shouldBe "register · payment"
        orderSummary(OrderConfig(type = OrderType.AFT)) shouldBe "register · aft"
        orderSummary(OrderConfig(source = OrderSource.MD_ORDER, mdOrder = "01ea8a09")) shouldBe "mdOrder · 01ea8a09"
        orderSummary(OrderConfig(source = OrderSource.MD_ORDER)) shouldBe "mdOrder · empty"
        orderSummary(OrderConfig(source = OrderSource.SESSION_ID, sessionId = "ps_1")) shouldBe "sessionId · ps_1"
    }

    @Test
    fun `the amount row reads in major units with the currency`() {
        amountSummary(OrderConfig()) shouldBe "100.00 EUR"
        amountSummary(OrderConfig(amount = 5, currency = "USD")) shouldBe "0.05 USD"
    }

    @Test
    fun `features, extra and the client id read none when empty`() {
        featuresSummary(OrderConfig()) shouldBe "none"
        featuresSummary(OrderConfig(features = listOf("VERIFY", "FORCE_TDS"))) shouldBe "VERIFY, FORCE_TDS"
        extraSummary(OrderConfig()) shouldBe "none"
        extraSummary(OrderConfig(extra = JsonObject(mapOf("description" to JsonPrimitive("QA"))))) shouldBe "description"
        clientIdSummary(OrderConfig()) shouldBe "none"
        clientIdSummary(OrderConfig(clientId = "c1")) shouldBe "c1"
    }

    @Test
    fun `the behaviour row lists the locale and the switches that are on`() {
        behaviourSummary(SdkOptions()) shouldBe "system locale"
        behaviourSummary(SdkOptions(locale = "en", showResultScreen = true, scanner = true, keyProviderUrl = "https://k")) shouldBe
            "en · result · scanner · key provider"
    }

    @Test
    fun `google pay and pinning read off until switched on`() {
        googlePaySummary(SdkOptions()) shouldBe "off"
        googlePaySummary(SdkOptions(googlePay = GooglePayOptions(enabled = true))) shouldBe "on"
        googlePaySummary(SdkOptions(googlePay = GooglePayOptions(enabled = true, environment = "test"))) shouldBe "on · test"
        sslPinningSummary(SdkOptions()) shouldBe "off"
        sslPinningSummary(SdkOptions(sslPinning = SslPinningOptions(enabled = true))) shouldBe "0 pins"
        sslPinningSummary(SdkOptions(sslPinning = SslPinningOptions(enabled = true, pins = listOf(pin)))) shouldBe "1 pin"
        sslPinningSummary(SdkOptions(sslPinning = SslPinningOptions(enabled = true, pins = listOf(pin, pin)))) shouldBe "2 pins"
    }

    @Test
    fun `a stand reads as its host and api`() {
        standSummary(StandProfile("sandbox", "https://dev.bpcbt.com/payment", ApiVersion.V2)) shouldBe "dev.bpcbt.com · API v2"
    }

    @Test
    fun `the last run reads as its minute and status`() {
        val utc = TimeZone.getTimeZone("UTC")
        val time = 10L * 3_600_000 + 50L * 60_000 + 30_000
        lastRunSummary(time, OrderStatus.Known(2, 0, null), utc) shouldBe "10:50 · status 2 · action 0"
        lastRunSummary(time, OrderStatus.NotQueried, utc) shouldBe "10:50 · status not queried"
    }

    @Test
    fun `the banner details the status`() {
        statusDetail(OrderStatus.Known(2, 0, "Success")) shouldBe listOf("orderStatus 2 · actionCode 0", "Success")
        statusDetail(OrderStatus.NotQueried) shouldBe listOf("status not queried")
    }

    @Test
    fun `a launched order reads as its type and amount`() {
        launchedOrderSummary(OrderConfig()) shouldBe "payment · 100.00 EUR"
        launchedOrderSummary(OrderConfig(source = OrderSource.SESSION_ID, sessionId = "ps_1")) shouldBe "sessionId · ps_1"
    }

    @Test
    fun `outcomes have a title and a tone`() {
        outcomeTitle(ResultType.REGISTRATION_FAILED) shouldBe "Registration failed"
        outcomeTone(ResultType.COMPLETED) shouldBe Tone.OK
        outcomeTone(ResultType.SDK_ERROR) shouldBe Tone.BAD
        outcomeTone(ResultType.CANCELED) shouldBe Tone.NEUTRAL
    }

    @Test
    fun `a problem is shown on the row whose screen fixes it`() {
        homeRow("profile") shouldBe HomeRow.STAND
        homeRow("profile.apiKey") shouldBe HomeRow.STAND
        homeRow("order.amount") shouldBe HomeRow.AMOUNT
        homeRow("order.features") shouldBe HomeRow.FEATURES
        homeRow("order.mdOrder") shouldBe HomeRow.ORDER
        homeRow("sdk.theme") shouldBe HomeRow.THEME
        homeRow("sdk.googlePay.environment") shouldBe HomeRow.GOOGLE_PAY
        homeRow("sdk.sslPinning.pins") shouldBe HomeRow.SSL_PINNING
        homeRow("sdk.locale") shouldBe HomeRow.BEHAVIOUR
    }

    @Test
    fun `a row problem gets its field's name when the message needs a subject`() {
        rowProblem(Problem("order.mdOrder", "is required")) shouldBe "mdOrder is required"
        rowProblem(Problem("profile", "select a stored profile")) shouldBe "select a stored profile"
    }

    @Test
    fun `the order screen shows the fields its source uses`() {
        orderFields(OrderSource.REGISTER) shouldBe
            listOf(Tags.ORDER_TYPE, Tags.ORDER_AMOUNT, Tags.ORDER_CURRENCY, Tags.ORDER_FEATURES, Tags.ORDER_CLIENT_ID, Tags.ORDER_EXTRA)
        orderFields(OrderSource.MD_ORDER) shouldBe listOf(Tags.ORDER_MD_ORDER)
        orderFields(OrderSource.SESSION_ID) shouldBe listOf(Tags.ORDER_SESSION_ID)
    }

    @Test
    fun `durations keep a decimal under ten seconds`() {
        durationText(4_321) shouldBe "4.3 s"
        durationText(0) shouldBe "0.0 s"
        durationText(20_400) shouldBe "20 s"
    }
}
