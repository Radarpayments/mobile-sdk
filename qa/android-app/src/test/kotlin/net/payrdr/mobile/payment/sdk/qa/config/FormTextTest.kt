package net.payrdr.mobile.payment.sdk.qa.config

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class FormTextTest {
    private val pin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    @Test
    fun `names are split on commas and trimmed`() {
        namesFromText(" VERIFY ,FORCE_CREATE_BINDING,, ") shouldBe listOf("VERIFY", "FORCE_CREATE_BINDING")
    }

    @Test
    fun `a pin line is split at the first equals sign`() {
        pinsFromText("dev.bpcbt.com=$pin") shouldBe Decoded.Ok(listOf(Pin("dev.bpcbt.com", pin)))
    }

    @Test
    fun `blank pin lines are skipped`() {
        pinsFromText("\n\n") shouldBe Decoded.Ok(emptyList<Pin>())
        pinsFromText("a.example=$pin\r\n  \r\nb.example=$pin\n") shouldBe
            Decoded.Ok(listOf(Pin("a.example", pin), Pin("b.example", pin)))
    }

    @Test
    fun `a pin line without a host names the pins field`() {
        (pinsFromText("=AAAA") as Decoded.Invalid).problem.field shouldBe "sdk.sslPinning.pins"
    }

    @Test
    fun `a pin line that is not a pin names the pins field`() {
        (pinsFromText("dev.bpcbt.com") as Decoded.Invalid).problem.field shouldBe "sdk.sslPinning.pins"
        (pinsFromText("dev.bpcbt.com=AAAA") as Decoded.Invalid).problem.field shouldBe "sdk.sslPinning.pins"
    }

    @Test
    fun `blank extra text is an empty object`() {
        extraFromText("  ") shouldBe Decoded.Ok(JsonObject(emptyMap()))
    }

    @Test
    fun `extra text that is not an object names the field`() {
        (extraFromText("[1]") as Decoded.Invalid).problem.field shouldBe "order.extra"
        (extraFromText("{") as Decoded.Invalid).problem.field shouldBe "order.extra"
    }

    @Test
    fun `extra text with a forbidden key names the field`() {
        (extraFromText("""{"Password":"x"}""") as Decoded.Invalid).problem.field shouldBe "order.extra"
    }

    @Test
    fun `pins and names survive the round trip through text`() {
        val pins = listOf(Pin("a.example", pin), Pin("b.example", pin))
        pinsFromText(pinsToText(pins)) shouldBe Decoded.Ok(pins)
        val names = listOf("VERIFY", "FORCE_CREATE_BINDING")
        namesFromText(namesToText(names)) shouldBe names
        val extra = JsonObject(mapOf("language" to JsonPrimitive("en")))
        extraFromText(extraToText(extra)) shouldBe Decoded.Ok(extra)
        extraFromText(extraToText(JsonObject(emptyMap()))) shouldBe Decoded.Ok(JsonObject(emptyMap()))
    }
}
