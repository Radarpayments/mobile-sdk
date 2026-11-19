package net.payrdr.mobile.payment.sdk.qa.ui

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.Pin
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.SdkOptions
import net.payrdr.mobile.payment.sdk.qa.config.SslPinningOptions
import net.payrdr.mobile.payment.sdk.qa.config.ThemePreset
import kotlin.test.Test

class ConfigFormTest {
    private val form = ConfigForm(QaConfig())
    private val pin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    @Test
    fun `the amount is typed in major units and stored in minor units`() {
        form.edit(Tags.ORDER_AMOUNT, "2.50")

        form.config.order.amount shouldBe 250
        form.problem shouldBe null
    }

    @Test
    fun `the amount reads in major units of the currency`() {
        form.text(Tags.ORDER_AMOUNT) shouldBe "100.00"
    }

    @Test
    fun `more decimals than the currency has names the field`() {
        form.edit(Tags.ORDER_AMOUNT, "2.505")

        form.config.order.amount shouldBe 10_000
        form.problem?.field shouldBe "order.amount"
    }

    @Test
    fun `a half typed amount keeps the last good config and names the field`() {
        form.edit(Tags.ORDER_AMOUNT, "25x")

        form.config.order.amount shouldBe 10_000
        form.text(Tags.ORDER_AMOUNT) shouldBe "25x"
        form.problem?.field shouldBe "order.amount"
    }

    @Test
    fun `an amount out of range names the field`() {
        form.edit(Tags.ORDER_AMOUNT, "10000000000")

        form.config.order.amount shouldBe 10_000
        form.problem?.field shouldBe "order.amount"
    }

    @Test
    fun `the amount takes ASCII digits only`() {
        listOf("+500", "-500", "５００", "٥٠٠", " 500", "5 00", ".50", "1.2.3").forEach { text ->
            form.edit(Tags.ORDER_AMOUNT, text)

            form.config.order.amount shouldBe 10_000
            form.problem?.field shouldBe "order.amount"
        }
    }

    @Test
    fun `correcting the text clears the problem`() {
        form.edit(Tags.ORDER_AMOUNT, "25x")
        form.edit(Tags.ORDER_AMOUNT, "25")

        form.problem shouldBe null
        form.config.order.amount shouldBe 2_500
    }

    @Test
    fun `the text keeps what was typed while it is valid`() {
        form.edit(Tags.ORDER_FEATURES, "VERIFY,")

        form.text(Tags.ORDER_FEATURES) shouldBe "VERIFY,"
        form.config.order.features shouldBe listOf("VERIFY")
    }

    @Test
    fun `features are typed as a comma separated line`() {
        form.edit(Tags.ORDER_FEATURES, "VERIFY, FORCE_CREATE_BINDING")

        form.config.order.features shouldBe listOf("VERIFY", "FORCE_CREATE_BINDING")
    }

    @Test
    fun `a feature with a space inside names the field`() {
        form.edit(Tags.ORDER_FEATURES, "VERIFY BINDING")

        form.config.order.features shouldBe emptyList()
        form.problem?.field shouldBe "order.features"
    }

    @Test
    fun `extra is typed as a json object`() {
        form.edit(Tags.ORDER_EXTRA, """{"description":"QA"}""")

        form.config.order.extra shouldBe JsonObject(mapOf("description" to JsonPrimitive("QA")))
    }

    @Test
    fun `extra with a forbidden key is not applied`() {
        form.edit(Tags.ORDER_EXTRA, """{"password":"x"}""")

        form.config.order.extra shouldBe JsonObject(emptyMap())
        form.problem?.field shouldBe "order.extra"
    }

    @Test
    fun `pins are typed one per line`() {
        form.edit(Tags.SDK_SSL_PINNING_PINS, "a.example=$pin\nb.example=$pin")

        form.config.sdk.sslPinning.pins shouldBe listOf(Pin("a.example", pin), Pin("b.example", pin))
    }

    @Test
    fun `a pin that is not a pin names the field`() {
        form.edit(Tags.SDK_SSL_PINNING_PINS, "a.example=AAAA")

        form.config.sdk.sslPinning.pins shouldBe emptyList()
        form.problem?.field shouldBe "sdk.sslPinning.pins"
    }

    @Test
    fun `an empty optional text is null`() {
        form.edit(Tags.ORDER_CLIENT_ID, "client-1")
        form.edit(Tags.ORDER_CLIENT_ID, "")

        form.config.order.clientId shouldBe null
    }

    @Test
    fun `a selector takes the json value`() {
        form.edit(Tags.ORDER_SOURCE, "mdOrder")
        form.edit(Tags.SDK_THEME, "largeText")

        form.config.order.source shouldBe OrderSource.MD_ORDER
        form.config.sdk.theme shouldBe ThemePreset.LARGE_TEXT
        form.text(Tags.SDK_THEME) shouldBe "largeText"
    }

    @Test
    fun `a value a selector does not offer names the field`() {
        form.edit(Tags.SDK_LOCALE, "xx")

        form.config.sdk.locale shouldBe null
        form.problem?.field shouldBe "sdk.locale"
    }

    @Test
    fun `a switch takes true and false`() {
        form.edit(Tags.SDK_SCANNER, "true")
        form.edit(Tags.SDK_SSL_PINNING_ENABLED, "true")

        form.config.sdk.scanner shouldBe true
        form.config.sdk.sslPinning.enabled shouldBe true
    }

    @Test
    fun `a wallet set is kept in the order of the contract`() {
        form.edit(Tags.SDK_GOOGLE_PAY_ALLOWED_CARD_NETWORKS, "VISA, AMEX")

        form.config.sdk.googlePay.allowedCardNetworks shouldBe listOf("AMEX", "VISA")
    }

    @Test
    fun `a wallet set with an unknown name names the field`() {
        form.edit(Tags.SDK_GOOGLE_PAY_ALLOWED_AUTH_METHODS, "PAN_ONLY, OTHER")

        form.config.sdk.googlePay.allowedAuthMethods shouldBe listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
        form.problem?.field shouldBe "sdk.googlePay.allowedAuthMethods"
    }

    @Test
    fun `a profile name outside the pattern names the field`() {
        form.edit(Tags.PROFILE, "Dev Stand")

        form.config.profile shouldBe null
        form.problem?.field shouldBe "profile"
    }

    @Test
    fun `the first pending problem is the first field of the form`() {
        form.edit(Tags.SDK_LOCALE, "xx")
        form.edit(Tags.ORDER_AMOUNT, "x")

        form.problem?.field shouldBe "order.amount"
    }

    @Test
    fun `setting a config drops the drafts`() {
        form.edit(Tags.ORDER_AMOUNT, "x")

        form.set(QaConfig())

        form.problem shouldBe null
        form.text(Tags.ORDER_AMOUNT) shouldBe "100.00"
    }

    @Test
    fun `every field tag of the contract can be edited`() {
        val config =
            QaConfig(
                profile = "dev",
                order = OrderConfig(clientId = "c", features = listOf("VERIFY"), extra = JsonObject(mapOf("a" to JsonPrimitive(1)))),
                sdk = SdkOptions(locale = "de", sslPinning = SslPinningOptions(true, listOf(Pin("dev.bpcbt.com", pin)))),
            )
        listOf(QaConfig(), config).forEach { start ->
            val form = ConfigForm(start)
            Tags.fields.forEach { tag ->
                form.edit(tag, form.text(tag))

                form.problem shouldBe null
                form.config shouldBe start
            }
        }
    }
}
