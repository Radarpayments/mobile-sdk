package net.payrdr.mobile.payment.sdk.payment.ui.screens

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_failure_hint
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_session_expired
import kotlin.test.Test

class FailureChipTest {
    @Test
    fun `a bank decline already said under the amount gets no chip`() {
        val reason = "Operation declined, please contact the store."
        failureChipText(description = reason, error = reason) shouldBe null
        failureChipText(description = reason, error = " operation declined, please contact the store. ") shouldBe null
    }

    @Test
    fun `a technical error unlike the description keeps its chip`() {
        failureChipText(description = null, error = "Payment finished with status CREATED") shouldBe "Payment finished with status CREATED"
        failureChipText(description = "Declined", error = "Payment finished with status CREATED") shouldBe
            "Payment finished with status CREATED"
    }

    @Test
    fun `no error - no chip`() {
        failureChipText(description = "Declined", error = null) shouldBe null
        failureChipText(description = "Declined", error = "  ") shouldBe null
    }

    @Test
    fun `an expired session says so instead of suggesting another card and gets no chip`() {
        val expired = SdkSessionExpiredException("md-1")
        failureHint(expired) shouldBe Res.string.result_session_expired
        failureChip(description = null, error = expired) shouldBe null
        failureChip(description = null, error = SdkDeclinedException(message = "Declined")) shouldBe "Declined"
    }

    @Test
    fun `other failures suggest another card`() {
        failureHint(SdkDeclinedException(message = "Declined")) shouldBe Res.string.result_failure_hint
        failureHint(null) shouldBe Res.string.result_failure_hint
    }
}
