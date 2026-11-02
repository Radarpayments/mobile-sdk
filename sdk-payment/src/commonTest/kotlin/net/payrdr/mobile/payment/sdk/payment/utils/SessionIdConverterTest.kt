package net.payrdr.mobile.payment.sdk.payment.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.exception.SdkInvalidSessionIdException
import kotlin.test.Test

class SessionIdConverterTest {
    @Test
    fun `mdOrderToSessionId then sessionIdToMdOrder round-trips the value`() {
        val mdOrder = "7f472085-399e-414e-b51c-a7b538aee497"
        val sessionId = SessionIdConverter.mdOrderToSessionId(mdOrder)

        sessionId.startsWith("ps_") shouldBe true
        SessionIdConverter.sessionIdToMdOrder(sessionId) shouldBe mdOrder
    }

    @Test
    fun `ids match the ones the API V2 gateway issues`() {
        // A session created on dev.bpcbt.com/api2 and the mdOrder of its paymentUrl.
        val mdOrder = "0180a01a-a8cb-75b8-8f9d-1a5f00ab8a11"
        val sessionId = "ps_2GRSigK2sUQKFSAZWbg2z7UuQxxjz7wqCjZqYyWRiq22aF8xxCDPua"

        SessionIdConverter.mdOrderToSessionId(mdOrder) shouldBe sessionId
        SessionIdConverter.sessionIdToMdOrder(sessionId) shouldBe mdOrder
    }

    @Test
    fun `sessionIdToMdOrder throws on blank input`() {
        shouldThrow<SdkInvalidSessionIdException> { SessionIdConverter.sessionIdToMdOrder("") }
    }

    @Test
    fun `sessionIdToMdOrder throws on missing ps underscore prefix`() {
        shouldThrow<SdkInvalidSessionIdException> { SessionIdConverter.sessionIdToMdOrder("ab_xyz") }
    }

    @Test
    fun `sessionIdToMdOrder throws on non-base58 body`() {
        shouldThrow<SdkInvalidSessionIdException> { SessionIdConverter.sessionIdToMdOrder("ps_0OIl") }
    }
}
