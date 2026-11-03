package net.payrdr.mobile.payment.sdk.payment.threeds

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ThreeDsRedirectTest {
    @Test fun `a return without an error is a success with its query`() {
        ThreeDsRedirect.result(listOf("MD" to "md-1", "PaRes" to "pares")) shouldBe
            ThreeDsResult.Succeeded(mapOf("MD" to "md-1", "PaRes" to "pares"))
    }

    @Test fun `an error parameter fails with its value whatever its case`() {
        ThreeDsRedirect.result(listOf("MD" to "md-1", "error" to "declined")) shouldBe ThreeDsResult.Failed("declined")
        ThreeDsRedirect.result(listOf("Error" to "declined")) shouldBe ThreeDsResult.Failed("declined")
        ThreeDsRedirect.result(listOf("ERROR" to "")) shouldBe ThreeDsResult.Failed("unknown")
    }
}
