package net.payrdr.mobile.payment.sdk.payment.threeds

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ThreeDsChallengeTest {
    private val sessionId = "ps_2GRSigK2sUQKKtDNCrx5sxLtE81daMGQCAT2SLXot9z58PHEtJatji"

    private fun challenge(sessionId: String? = this.sessionId) =
        ThreeDsChallenge(
            mdOrder = "0180a598-a9da-7e5e-918b-f17700ab8a11",
            acsUrl = "https://dev.bpcbt.com/payment/rest/getwhitepageurl.do",
            paReq = "PA",
            md = "0180a598-a9da-7e5e-918b-f17700ab8a11",
            termUrl = "https://t",
            sessionId = sessionId,
        )

    @Test fun `the return prefix ends the challenge`() {
        challenge().isReturnUrl("sdk://done?sessionId=$sessionId&lang=en") shouldBe true
    }

    @Test fun `the session's own success and failure pages end the challenge`() {
        challenge().isReturnUrl("https://shop.example/ok?sessionId=$sessionId&lang=en") shouldBe true
        challenge().isReturnUrl("https://shop.example/ok?x=1&sessionId=$sessionId") shouldBe true
        challenge().isReturnUrl("https://shop.example/fail?lang=en&sessionId=$sessionId#top") shouldBe true
    }

    @Test fun `bank and gateway pages on the way do not end it`() {
        val c = challenge()
        val whitePage = "https://dev.bpcbt.com/payment/modules/verify-3ds2/collect-data-3ds2.html?orderId=${c.mdOrder}&lang=en"
        c.isReturnUrl(whitePage) shouldBe false
        c.isReturnUrl("https://acs.example/acs2/acs/creq") shouldBe false
        c.isReturnUrl("https://dev.bpcbt.com/payment/rest/finish3dsVer2.do?MD=${c.mdOrder}") shouldBe false
    }

    @Test fun `another session's page does not end it`() {
        challenge().isReturnUrl("https://shop.example/ok?sessionId=${sessionId}x") shouldBe false
        challenge().isReturnUrl("https://shop.example/ok?otherSessionId=$sessionId") shouldBe false
    }

    @Test fun `without a session id only the prefix ends it`() {
        challenge(sessionId = null).isReturnUrl("https://shop.example/ok?sessionId=$sessionId") shouldBe false
        challenge(sessionId = null).isReturnUrl("sdk://done?orderId=1") shouldBe true
    }
}
