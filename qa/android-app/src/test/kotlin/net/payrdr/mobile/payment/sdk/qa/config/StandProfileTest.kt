package net.payrdr.mobile.payment.sdk.qa.config

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class StandProfileTest {
    private val profile =
        StandProfile("s", "https://dev.bpcbt.com/payment", login = "login-1", password = "password-1", apiKey = "key-1")

    @Test
    fun `toString hides the credentials`() {
        val text = profile.toString()
        text shouldNotContain "login-1"
        text shouldNotContain "password-1"
        text shouldNotContain "key-1"
        text shouldBe "StandProfile(name=s, gatewayUrl=https://dev.bpcbt.com/payment, api=V1)"
    }

    @Test
    fun `the encoded profile carries them`() {
        val encoded = StandProfileJson.encode(profile)
        encoded.contains("password-1") shouldBe true
        StandProfileJson.decode(encoded) shouldBe Decoded.Ok(profile)
    }

    @Test
    fun `a problem about a profile never contains its credentials`() {
        val bad = profile.copy(gatewayUrl = "not a url")
        val problem = StandProfileJson.problemOf(bad)
        problem?.field shouldBe "gatewayUrl"
        problem.toString() shouldNotContain "password-1"
        problem.toString() shouldNotContain "key-1"
    }

    @Test
    fun `a profile of the other api version round trips`() {
        val v2 = profile.copy(api = ApiVersion.V2)
        StandProfileJson.decode(StandProfileJson.encode(v2)) shouldBe Decoded.Ok(v2)
    }
}
