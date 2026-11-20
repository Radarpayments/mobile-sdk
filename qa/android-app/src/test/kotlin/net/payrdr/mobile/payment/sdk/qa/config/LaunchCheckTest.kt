package net.payrdr.mobile.payment.sdk.qa.config

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LaunchCheckTest {
    private val v1 = StandProfile("sandbox", "https://dev.bpcbt.com/payment", ApiVersion.V1, "login-1", "password-1")
    private val v2 = StandProfile("sandbox", "https://dev.bpcbt.com/payment", ApiVersion.V2, apiKey = "key-1")

    private fun config(
        source: OrderSource = OrderSource.REGISTER,
        type: OrderType = OrderType.PAYMENT,
        mdOrder: String? = null,
        sessionId: String? = null,
        pinning: SslPinningOptions = SslPinningOptions(),
    ) = QaConfig(
        profile = "sandbox",
        order = OrderConfig(source = source, type = type, mdOrder = mdOrder, sessionId = sessionId),
        sdk = SdkOptions(sslPinning = pinning),
    )

    @Test
    fun `a v1 profile with credentials and a default config has no problem`() {
        launchProblem(QaConfig(profile = "sandbox"), v1) shouldBe null
    }

    @Test
    fun `no profile selected is a problem of the profile field`() {
        launchProblem(QaConfig(profile = null), v1)?.field shouldBe "profile"
    }

    @Test
    fun `a profile that is not stored is a problem of the profile field`() {
        launchProblem(QaConfig(profile = "gone"), null)?.field shouldBe "profile"
    }

    @Test
    fun `aft registration on a v2 profile is a problem of the type field`() {
        launchProblem(config(type = OrderType.AFT), v2)?.field shouldBe "order.type"
    }

    @Test
    fun `aft with an existing session on a v2 profile is allowed`() {
        launchProblem(config(type = OrderType.AFT, source = OrderSource.SESSION_ID, sessionId = "s"), v2) shouldBe null
        launchProblem(config(type = OrderType.AFT, source = OrderSource.MD_ORDER, mdOrder = "x"), v2) shouldBe null
    }

    @Test
    fun `registering on v1 needs a login and a password`() {
        launchProblem(config(), v1.copy(login = ""))?.field shouldBe "profile.login"
        launchProblem(config(), v1.copy(password = " "))?.field shouldBe "profile.login"
    }

    @Test
    fun `registering on v2 needs an api key`() {
        launchProblem(config(), v2.copy(apiKey = ""))?.field shouldBe "profile.apiKey"
    }

    @Test
    fun `opening an existing order needs no credentials`() {
        launchProblem(config(source = OrderSource.MD_ORDER, mdOrder = "x"), v1.copy(login = "", password = "")) shouldBe null
        launchProblem(config(source = OrderSource.SESSION_ID, sessionId = "x"), v2.copy(apiKey = "")) shouldBe null
    }

    @Test
    fun `an md order source without an md order is a problem of that field`() {
        launchProblem(config(source = OrderSource.MD_ORDER), v1)?.field shouldBe "order.mdOrder"
    }

    @Test
    fun `a session id source without a session id is a problem of that field`() {
        launchProblem(config(source = OrderSource.SESSION_ID), v1)?.field shouldBe "order.sessionId"
    }

    @Test
    fun `pinning without pins is a problem of the pins field`() {
        launchProblem(config(pinning = SslPinningOptions(enabled = true)), v1)?.field shouldBe "sdk.sslPinning.pins"
    }

    @Test
    fun `pinning with a pin has no problem`() {
        val pins = listOf(Pin("dev.bpcbt.com", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="))
        launchProblem(config(pinning = SslPinningOptions(enabled = true, pins = pins)), v1) shouldBe null
    }

    @Test
    fun `the first failing check in the contract order is reported`() {
        val broken = config(type = OrderType.AFT, pinning = SslPinningOptions(enabled = true))
        launchProblem(broken, v2.copy(apiKey = ""))?.field shouldBe "order.type"
        launchProblem(broken, null)?.field shouldBe "profile"
        launchProblem(config(source = OrderSource.MD_ORDER, pinning = SslPinningOptions(enabled = true)), v1)?.field shouldBe
            "order.mdOrder"
    }
}
