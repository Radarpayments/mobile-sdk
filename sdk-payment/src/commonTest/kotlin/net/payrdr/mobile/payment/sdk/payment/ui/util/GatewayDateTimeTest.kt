package net.payrdr.mobile.payment.sdk.payment.ui.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GatewayDateTimeTest {
    @Test
    fun `the gateway's day-first date with seconds`() {
        GatewayDateTime.parse("22.10.2026 14:04:07") shouldBe GatewayDateTime(year = 2026, month = 10, day = 22, hour = 14, minute = 4)
    }

    @Test
    fun `seconds are optional and the year-first form is read too`() {
        GatewayDateTime.parse("01.02.2027 09:30") shouldBe GatewayDateTime(year = 2027, month = 2, day = 1, hour = 9, minute = 30)
        GatewayDateTime.parse("2026-10-22 14:04:07") shouldBe GatewayDateTime(year = 2026, month = 10, day = 22, hour = 14, minute = 4)
    }

    @Test
    fun `anything else is not a date`() {
        GatewayDateTime.parse(null) shouldBe null
        GatewayDateTime.parse("") shouldBe null
        GatewayDateTime.parse("yesterday") shouldBe null
        GatewayDateTime.parse("32.13.2026 25:61") shouldBe null
    }
}
