package net.payrdr.mobile.payment.sdk.qa.intake

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import java.util.Base64
import kotlin.test.Test

class IntakeTest {
    private val configJson = """{"profile":"sandbox","order":{"amount":500}}"""
    private val config = QaConfig(profile = "sandbox", order = OrderConfig(amount = 500))

    // Two bytes of input leave one padding character, so the padded and unpadded spellings differ.
    private val paddedConfigJson = """{"order":{"amount":5}}"""
    private val paddedConfig = QaConfig(order = OrderConfig(amount = 5))

    private val profileJson =
        """{"name":"sandbox","gatewayUrl":"https://dev.bpcbt.com/payment","api":"v2","apiKey":"key-1"}"""
    private val profile = StandProfile("sandbox", "https://dev.bpcbt.com/payment", api = ApiVersion.V2, apiKey = "key-1")

    private fun b64(text: String): String = Base64.getUrlEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))

    private fun field(result: List<Intake>): String? = (result.singleOrNull() as? Intake.Rejected)?.problem?.field

    @Test
    fun `a config link yields the config`() {
        intakeFromLink("payrdrqa://config?data=${b64(configJson)}") shouldBe listOf(Intake.Config(config))
    }

    @Test
    fun `launch=1 adds a launch after the config`() {
        intakeFromLink("payrdrqa://config?data=${b64(configJson)}&launch=1") shouldBe
            listOf(Intake.Config(config), Intake.Launch)
        intakeFromLink("payrdrqa://config?launch=1&data=${b64(configJson)}") shouldBe
            listOf(Intake.Config(config), Intake.Launch)
    }

    @Test
    fun `a launch value other than 1 is not a launch`() {
        listOf("launch=0", "launch=true", "launch=", "launch", "launch=11", "launch=1&launch=1", "LAUNCH=1").forEach {
            intakeFromLink("payrdrqa://config?data=${b64(configJson)}&$it") shouldBe listOf(Intake.Config(config))
        }
    }

    @Test
    fun `a profile link asks for confirmation`() {
        intakeFromLink("payrdrqa://profile?data=${b64(profileJson)}") shouldBe
            listOf(Intake.Profile(profile, needsConfirmation = true))
    }

    @Test
    fun `a profile link does not launch`() {
        intakeFromLink("payrdrqa://profile?data=${b64(profileJson)}&launch=1") shouldBe
            listOf(Intake.Profile(profile, needsConfirmation = true))
    }

    @Test
    fun `padded and unpadded base64url are both read`() {
        val padded = b64(paddedConfigJson)
        padded.endsWith("=") shouldBe true
        intakeFromLink("payrdrqa://config?data=$padded") shouldBe listOf(Intake.Config(paddedConfig))
        intakeFromLink("payrdrqa://config?data=${padded.trimEnd('=')}") shouldBe listOf(Intake.Config(paddedConfig))
    }

    @Test
    fun `a percent-encoded data value is read`() {
        val padded = b64(paddedConfigJson)
        intakeFromLink("payrdrqa://config?data=${padded.replace("=", "%3D")}") shouldBe listOf(Intake.Config(paddedConfig))
        intakeFromLink("payrdrqa://config?data=${padded.replace("=", "%3d")}") shouldBe listOf(Intake.Config(paddedConfig))
    }

    @Test
    fun `the url-safe characters of base64url are read`() {
        // The text is chosen so that its base64 has both a '+' and a '/' that base64url spells '-' and '_'.
        val json = """{"order":{"clientId":">>>???"}}"""
        val encoded = b64(json)
        (encoded.contains('-') && encoded.contains('_')) shouldBe true
        intakeFromLink("payrdrqa://config?data=$encoded") shouldBe
            listOf(Intake.Config(QaConfig(order = OrderConfig(clientId = ">>>???"))))
    }

    @Test
    fun `standard base64 is rejected because the contract names base64url only`() {
        val json = """{"order":{"clientId":">>>???"}}"""
        val standard = Base64.getEncoder().encodeToString(json.toByteArray())
        field(intakeFromLink("payrdrqa://config?data=$standard")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?data=${standard.replace("+", "%2B").replace("/", "%2F")}")) shouldBe "data"
    }

    @Test
    fun `wrong padding is rejected`() {
        val unpadded = b64(paddedConfigJson).trimEnd('=')
        field(intakeFromLink("payrdrqa://config?data=$unpadded===")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?data=${unpadded.dropLast(1)}=${unpadded.last()}")) shouldBe "data"
    }

    @Test
    fun `an invalid config link is rejected with its field and does not launch`() {
        val link = "payrdrqa://config?data=${b64("""{"order":{"amount":-1}}""")}&launch=1"
        field(intakeFromLink(link)) shouldBe "order.amount"
    }

    @Test
    fun `an invalid profile link is rejected with the decoder's own problem`() {
        val json = """{"name":"Bad Name","gatewayUrl":"https://dev.bpcbt.com/payment","password":"password-1"}"""
        val result = intakeFromLink("payrdrqa://profile?data=${b64(json)}")
        field(result) shouldBe "name"
        result.toString() shouldNotContain "password-1"
    }

    @Test
    fun `a rejection does not echo the payload`() {
        val json = """{"order":{"extra":{"password":"password-1"}}}"""
        val link = "payrdrqa://config?data=${b64(json)}"
        val result = intakeFromLink(link)
        field(result) shouldBe "order.extra"
        result.toString() shouldNotContain "password-1"
        result.toString() shouldNotContain b64(json)
        field(intakeFromLink("payrdrqa://config?data=${b64(json)}!")).let { it shouldBe "data" }
        intakeFromLink("payrdrqa://config?data=${b64(json)}!").toString() shouldNotContain b64(json)
    }

    @Test
    fun `a link of another host is rejected`() {
        field(intakeFromLink("payrdrqa://settings?data=${b64(configJson)}")) shouldBe "link"
    }

    @Test
    fun `a link of another shape is rejected`() {
        listOf(
            "payrdrqa://config/extra?data=${b64(configJson)}",
            "payrdrqa://user@config?data=${b64(configJson)}",
            "payrdrqa://config:1?data=${b64(configJson)}",
            "payrdrqa://config?data=${b64(configJson)}#top",
            "https://config?data=${b64(configJson)}",
            "payrdrqa:config?data=${b64(configJson)}",
            "payrdrqa://",
            "",
        ).forEach { field(intakeFromLink(it)) shouldBe "link" }
    }

    @Test
    fun `a link without data is rejected`() {
        field(intakeFromLink("payrdrqa://config")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?launch=1")) shouldBe "data"
        field(intakeFromLink("payrdrqa://profile?")) shouldBe "data"
    }

    @Test
    fun `an empty data is rejected`() {
        field(intakeFromLink("payrdrqa://config?data=")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?data")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?data=&launch=1")) shouldBe "data"
    }

    @Test
    fun `a repeated data is rejected`() {
        field(intakeFromLink("payrdrqa://config?data=${b64(configJson)}&data=${b64(configJson)}")) shouldBe "data"
    }

    @Test
    fun `data that is not base64 is rejected`() {
        listOf("not base64", "{\"a\":1}", "QUJD!", "%ZZ", "%4", "QUJD%").forEach {
            field(intakeFromLink("payrdrqa://config?data=$it")) shouldBe "data"
        }
    }

    @Test
    fun `data whose bytes are not utf-8 is rejected`() {
        val bytes = byteArrayOf(0x7B, 0xC3.toByte(), 0x28, 0x7D)
        field(intakeFromLink("payrdrqa://config?data=${Base64.getUrlEncoder().encodeToString(bytes)}")) shouldBe "data"
    }

    @Test
    fun `data that is not a json object is rejected at the root`() {
        listOf("[]", "\"text\"", "1", "{").forEach {
            field(intakeFromLink("payrdrqa://config?data=${b64(it)}")) shouldBe "$"
        }
    }

    @Test
    fun `a link with surrounding whitespace is read`() {
        intakeFromLink("\n  payrdrqa://config?data=${b64(configJson)}&launch=1 \r\n") shouldBe
            listOf(Intake.Config(config), Intake.Launch)
    }

    @Test
    fun `extras apply the profile before the config and launch last`() {
        intakeFromExtras(qaConfig = configJson, qaProfile = profileJson, qaLaunch = true) shouldBe
            listOf(Intake.Profile(profile, needsConfirmation = false), Intake.Config(config), Intake.Launch)
    }

    @Test
    fun `qaLaunch alone launches the stored config`() {
        intakeFromExtras(qaConfig = null, qaProfile = null, qaLaunch = true) shouldBe listOf(Intake.Launch)
    }

    @Test
    fun `no extras yield nothing`() {
        intakeFromExtras(qaConfig = null, qaProfile = null, qaLaunch = false) shouldBe emptyList()
    }

    @Test
    fun `an invalid extra rejects every extra and does not launch`() {
        field(intakeFromExtras(qaConfig = """{"order":{"amount":-1}}""", qaProfile = profileJson, qaLaunch = true)) shouldBe
            "order.amount"
        field(intakeFromExtras(qaConfig = configJson, qaProfile = """{"name":"sandbox"}""", qaLaunch = true)) shouldBe
            "gatewayUrl"
        field(intakeFromExtras(qaConfig = "not json", qaProfile = null, qaLaunch = true)) shouldBe "$"
    }

    @Test
    fun `an empty extra is rejected as missing data`() {
        field(intakeFromExtras(qaConfig = "", qaProfile = null, qaLaunch = true)) shouldBe "data"
        field(intakeFromExtras(qaConfig = null, qaProfile = "  ", qaLaunch = false)) shouldBe "data"
    }

    @Test
    fun `pasted json with a gateway url is a profile to confirm`() {
        intakeFromText(profileJson) shouldBe listOf(Intake.Profile(profile, needsConfirmation = true))
    }

    @Test
    fun `pasted json without one is a config`() {
        intakeFromText(configJson) shouldBe listOf(Intake.Config(config))
    }

    @Test
    fun `pasted text is trimmed`() {
        intakeFromText("\n  $configJson\n") shouldBe listOf(Intake.Config(config))
        intakeFromText("\r\n\t$profileJson \n") shouldBe listOf(Intake.Profile(profile, needsConfirmation = true))
    }

    @Test
    fun `a pasted link is read as a link`() {
        intakeFromText("  payrdrqa://config?data=${b64(configJson)}&launch=1\n") shouldBe
            listOf(Intake.Config(config), Intake.Launch)
        intakeFromText("payrdrqa://profile?data=${b64(profileJson)}") shouldBe
            listOf(Intake.Profile(profile, needsConfirmation = true))
        field(intakeFromText("payrdrqa://other?data=${b64(configJson)}")) shouldBe "link"
    }

    @Test
    fun `pasted text that is neither is rejected`() {
        listOf("", "   ", "hello", "[1,2]", "https://dev.bpcbt.com/payment").forEach {
            field(intakeFromText(it)) shouldBe "$"
        }
    }

    @Test
    fun `a pasted profile that is invalid is rejected with its field`() {
        field(intakeFromText("""{"name":"sandbox","gatewayUrl":"ftp://dev.bpcbt.com"}""")) shouldBe "gatewayUrl"
    }
}
