package net.payrdr.mobile.payment.sdk.core.utils

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BindingUtilsTest {
    @Test
    fun `round-trip encode-prefix then extract returns the original`() {
        val raw = "cafecafe-cafe-cafe-cafe-cafecafecafe"
        val encoded = "pm_" + Base58Coder.encode(raw)

        BindingUtils.extractBindingId(encoded) shouldBe raw
    }

    @Test
    fun `non-pm prefix returns empty string`() {
        BindingUtils.extractBindingId("abc_def") shouldBe ""
    }

    @Test
    fun `invalid base58 body returns empty string`() {
        BindingUtils.extractBindingId("pm_0OIl") shouldBe ""
    }
}
