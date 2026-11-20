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
    fun `text that is not strict json is rejected at the root on every path`() {
        listOf("""{"order":{"amount":01}}""", """{"order":{"amount":+1}}""", """{"order":{"extra":{"a":abc}}}""", "{\"clientId\":\"a\tb\"}")
            .forEach {
                field(intakeFromText(it)) shouldBe "$"
                field(intakeFromLink("payrdrqa://config?data=${b64(it)}")) shouldBe "$"
                field(intakeFromExtras(qaConfig = it, qaProfile = null, qaLaunch = true)) shouldBe "$"
            }
        val profile = """{"name":"sandbox","gatewayUrl":"https://dev.bpcbt.com/payment","version":01}"""
        field(intakeFromText(profile)) shouldBe "$"
        field(intakeFromLink("payrdrqa://profile?data=${b64(profile)}")) shouldBe "$"
        field(intakeFromExtras(qaConfig = null, qaProfile = profile, qaLaunch = false)) shouldBe "$"
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

    // The root object is level 1, so depth n is n objects nested in each other.
    private fun nested(depth: Int): String = "{\"a\":".repeat(depth - 1) + "{}" + "}".repeat(depth - 1)

    // 'é' is two bytes of UTF-8 and one char, so the byte limit is not mistaken for a char limit.
    private fun sized(bytes: Int): String {
        val filler = bytes - 8
        return "{\"a\":\"" + "é".repeat(filler / 2) + "x".repeat(filler % 2) + "\"}"
    }

    @Test
    fun `a link or pasted text keeps the depth limit`() {
        intakeFromLink("payrdrqa://config?data=${b64(nested(32))}") shouldBe listOf(Intake.Config(QaConfig()))
        field(intakeFromLink("payrdrqa://config?data=${b64(nested(33))}&launch=1")) shouldBe "$"
        field(intakeFromLink("payrdrqa://profile?data=${b64(nested(33))}")) shouldBe "$"
        intakeFromText(nested(32)) shouldBe listOf(Intake.Config(QaConfig()))
        field(intakeFromText(nested(33))) shouldBe "$"
        field(intakeFromText(nested(100_000))) shouldBe "$"
        field(intakeFromExtras(qaConfig = nested(100_000), qaProfile = null, qaLaunch = true)) shouldBe "$"
    }

    @Test
    fun `a link or pasted text keeps the size limit in bytes`() {
        intakeFromLink("payrdrqa://config?data=${b64(sized(65536))}") shouldBe listOf(Intake.Config(QaConfig()))
        field(intakeFromLink("payrdrqa://config?data=${b64(sized(65537))}")) shouldBe "$"
        intakeFromText(sized(65536)) shouldBe listOf(Intake.Config(QaConfig()))
        field(intakeFromText(sized(65537))) shouldBe "$"
        field(intakeFromExtras(qaConfig = sized(65537), qaProfile = null, qaLaunch = false)) shouldBe "$"
    }

    @Test
    fun `data longer than base64url of the largest document is refused before it is decoded`() {
        // 87388 characters would decode to 65541 bytes; the largest document, 65536 bytes, takes 87384.
        field(intakeFromLink("payrdrqa://config?data=${"A".repeat(87_388)}")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?data=${"%41".repeat(87_388)}")) shouldBe "data"
        field(intakeFromText("payrdrqa://config?data=${"A".repeat(1_000_000)}")) shouldBe "data"
    }

    @Test
    fun `the scheme is read ignoring ascii case and the host is exact`() {
        intakeFromLink("PAYRDRQA://config?data=${b64(configJson)}&launch=1") shouldBe
            listOf(Intake.Config(config), Intake.Launch)
        intakeFromText("PayRdrQa://config?data=${b64(configJson)}") shouldBe listOf(Intake.Config(config))
        intakeFromText(" PAYRDRQA://profile?data=${b64(profileJson)}") shouldBe
            listOf(Intake.Profile(profile, needsConfirmation = true))
        field(intakeFromLink("payrdrqa://CONFIG?data=${b64(configJson)}")) shouldBe "link"
        field(intakeFromText("PAYRDRQA://Profile?data=${b64(profileJson)}")) shouldBe "link"
    }

    @Test
    fun `an authority other than the bare host is rejected`() {
        listOf(
            "payrdrqa://config:?data=${b64(configJson)}",
            "payrdrqa:///config?data=${b64(configJson)}",
            "payrdrqa://config/?data=${b64(configJson)}",
            "payrdrqa:config?data=${b64(configJson)}",
            "payrdrqa://%63onfig?data=${b64(configJson)}",
            "payrdrqa://config.?data=${b64(configJson)}",
        ).forEach { field(intakeFromLink(it)) shouldBe "link" }
    }

    @Test
    fun `trimming removes ascii space, tab, cr, lf and one leading byte order mark`() {
        intakeFromText("\uFEFF$configJson") shouldBe listOf(Intake.Config(config))
        intakeFromText("\uFEFF \t\r\n$configJson \t\r\n") shouldBe listOf(Intake.Config(config))
        intakeFromLink("\uFEFFpayrdrqa://config?data=${b64(configJson)}\n") shouldBe listOf(Intake.Config(config))
        intakeFromExtras(qaConfig = "\uFEFF$configJson\n", qaProfile = "\uFEFF$profileJson", qaLaunch = false) shouldBe
            listOf(Intake.Profile(profile, needsConfirmation = false), Intake.Config(config))
        field(intakeFromExtras(qaConfig = "\uFEFF \n", qaProfile = null, qaLaunch = false)) shouldBe "data"
    }

    @Test
    fun `other whitespace and a second byte order mark are not trimmed`() {
        field(intakeFromText("\u0085$configJson")) shouldBe "$"
        field(intakeFromText("$configJson\u0085")) shouldBe "$"
        field(intakeFromText("\u000B$configJson")) shouldBe "$"
        field(intakeFromText("\u00A0$configJson")) shouldBe "$"
        field(intakeFromText("\uFEFF\uFEFF$configJson")) shouldBe "$"
        field(intakeFromText("$configJson\uFEFF")) shouldBe "$"
        field(intakeFromText("\u0085payrdrqa://config?data=${b64(configJson)}")) shouldBe "$"
        field(intakeFromLink("\u0085payrdrqa://config?data=${b64(configJson)}")) shouldBe "link"
        field(intakeFromExtras(qaConfig = "\u0085$configJson", qaProfile = null, qaLaunch = false)) shouldBe "$"
        field(intakeFromExtras(qaConfig = "\u0085", qaProfile = null, qaLaunch = false)) shouldBe "$"
    }

    @Test
    fun `escapes that decode to nothing usable are rejected`() {
        field(intakeFromLink("payrdrqa://config?data=%00")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?data=${b64(configJson)}%00")) shouldBe "data"
        // C0 AE is an overlong spelling of '.', which a strict UTF-8 decoder refuses.
        field(intakeFromLink("payrdrqa://config?data=%C0%AE")) shouldBe "data"
        intakeFromLink("payrdrqa://config?data=${b64(configJson)}&launch=%C0%AE") shouldBe listOf(Intake.Config(config))
        intakeFromLink("payrdrqa://config?%C0%AE=1&data=${b64(configJson)}") shouldBe listOf(Intake.Config(config))
        intakeFromLink("payrdrqa://config?data=${b64(configJson)}&launch=%31") shouldBe
            listOf(Intake.Config(config), Intake.Launch)
    }

    @Test
    fun `an escape takes ascii hex digits only`() {
        // U+0664 and U+0661 are Arabic-Indic four and one, which a Unicode digit reader would turn into %41, 'A'.
        val data = b64(configJson)
        field(intakeFromLink("payrdrqa://config?data=${data.dropLast(1)}%٤١")) shouldBe "data"
        intakeFromLink("payrdrqa://config?data=$data&launch=%３１") shouldBe listOf(Intake.Config(config))
    }

    @Test
    fun `an overlong utf-8 sequence inside data is rejected`() {
        val bytes = byteArrayOf(0x7B, 0xC0.toByte(), 0xAE.toByte(), 0x7D)
        field(intakeFromLink("payrdrqa://config?data=${Base64.getUrlEncoder().encodeToString(bytes)}")) shouldBe "data"
    }

    @Test
    fun `base64url of the wrong length, with pad bits or a line break is rejected`() {
        val unpadded = b64(configJson).trimEnd('=')
        val oneLeft = if (unpadded.length % 4 == 0) unpadded + "A" else unpadded.dropLast(unpadded.length % 4 - 1)
        (oneLeft.length % 4) shouldBe 1
        field(intakeFromLink("payrdrqa://config?data=$oneLeft")) shouldBe "data"
        // `{"a":1}` is eyJhIjoxfQ==; 'R' sets the pad bits that 'Q' leaves zero.
        intakeFromLink("payrdrqa://config?data=eyJhIjoxfQ") shouldBe listOf(Intake.Config(QaConfig()))
        field(intakeFromLink("payrdrqa://config?data=eyJhIjoxfR")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?data=eyJhIjoxfQ=")) shouldBe "data"
        val half = unpadded.length / 2
        field(intakeFromLink("payrdrqa://config?data=${unpadded.take(half)}\n${unpadded.drop(half)}")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?data=${unpadded.take(half)}%0A${unpadded.drop(half)}")) shouldBe "data"
        field(intakeFromLink("payrdrqa://config?data=${unpadded.take(half)}%20${unpadded.drop(half)}")) shouldBe "data"
    }

    @Test
    fun `only an ampersand separates parameters and empty or nameless pairs are skipped`() {
        field(intakeFromLink("payrdrqa://config?data=${b64(configJson)};launch=1")) shouldBe "data"
        intakeFromLink("payrdrqa://config?data=${b64(configJson)}&a=1;launch=1") shouldBe listOf(Intake.Config(config))
        intakeFromLink("payrdrqa://config?=x&data=${b64(configJson)}&launch=1") shouldBe
            listOf(Intake.Config(config), Intake.Launch)
        intakeFromLink("payrdrqa://config?&&data=${b64(configJson)}&&launch=1&&") shouldBe
            listOf(Intake.Config(config), Intake.Launch)
        intakeFromLink("payrdrqa://config?data=${b64(configJson)}&other=%ZZ&x") shouldBe listOf(Intake.Config(config))
    }

    @Test
    fun `pasted bare base64 is not json`() {
        field(intakeFromText(b64(configJson))) shouldBe "$"
    }
}
