package net.payrdr.mobile.payment.sdk.payment.scanner.parser

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.NormalisedRect
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.RecognizedLine
import kotlin.test.Test

class CardParserTest {
    private val parser = CardParser()
    private val anywhere = NormalisedRect(0f, 0f, 1f, 1f)
    private val belowPan = NormalisedRect(0f, 0.6f, 1f, 0.7f)

    @Test
    fun `extracts PAN with spaces and matching expiry below it`() {
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("12/29", belowPan),
            )
        val card = parser.parse(lines, extractCardholder = false)
        card.shouldNotBeNull()
        card.pan shouldBe "4111111111111111"
        card.expiryMonth shouldBe 12
        card.expiryYear shouldBe 2029
        card.cardholder shouldBe null
    }

    @Test
    fun `extracts PAN with hyphen separators`() {
        val lines =
            listOf(
                RecognizedLine("4111-1111-1111-1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("01/30", NormalisedRect(0f, 0.6f, 1f, 0.7f)),
            )
        parser.parse(lines, false)!!.pan shouldBe "4111111111111111"
    }

    @Test
    fun `rejects PAN that fails Luhn`() {
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1112", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("01/30", NormalisedRect(0f, 0.6f, 1f, 0.7f)),
            )
        parser.parse(lines, false) shouldBe null
    }

    @Test
    fun `picks longest Luhn-valid PAN when multiple candidates present`() {
        val lines =
            listOf(
                RecognizedLine("4242 4242 4242 4242", NormalisedRect(0f, 0.3f, 1f, 0.4f)), // 16
                RecognizedLine("4000 0400 0000 0000 008", NormalisedRect(0f, 0.45f, 1f, 0.5f)), // 19, valid
                RecognizedLine("12/29", NormalisedRect(0f, 0.55f, 1f, 0.6f)),
            )
        parser.parse(lines, false)!!.pan shouldBe "4000040000000000008"
    }

    @Test
    fun `returns null when no expiry present`() {
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
            )
        parser.parse(lines, false) shouldBe null
    }

    @Test
    fun `rejects expiry strictly in the past`() {
        val past = CardParser(now = YearMonth(2030, 1))
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("12/29", NormalisedRect(0f, 0.6f, 1f, 0.7f)),
            )
        past.parse(lines, false) shouldBe null
    }

    @Test
    fun `cardholder null when extractCardholder is false`() {
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("JOHN APPLESEED", NormalisedRect(0f, 0.5f, 1f, 0.55f)),
                RecognizedLine("12/29", NormalisedRect(0f, 0.6f, 1f, 0.7f)),
            )
        parser.parse(lines, false)!!.cardholder shouldBe null
    }

    @Test
    fun `extracts cardholder when flag is true`() {
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("JOHN APPLESEED", NormalisedRect(0f, 0.5f, 1f, 0.55f)),
                RecognizedLine("12/29", NormalisedRect(0f, 0.6f, 1f, 0.7f)),
            )
        parser.parse(lines, true)!!.cardholder shouldBe "JOHN APPLESEED"
    }

    @Test
    fun `rejects cardholder matching issuer dictionary`() {
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("VALID THRU", NormalisedRect(0f, 0.5f, 1f, 0.55f)),
                RecognizedLine("12/29", NormalisedRect(0f, 0.6f, 1f, 0.7f)),
            )
        parser.parse(lines, true)!!.cardholder shouldBe null
    }

    @Test
    fun `returns null on pure garbage`() {
        val lines =
            listOf(
                RecognizedLine("THIS IS NOT A CARD", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
            )
        parser.parse(lines, true) shouldBe null
    }

    @Test
    fun `extracts 15-digit Amex PAN`() {
        val lines =
            listOf(
                RecognizedLine("3782 822463 10005", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("12/29", NormalisedRect(0f, 0.6f, 1f, 0.7f)),
            )
        parser.parse(lines, false)!!.pan shouldBe "378282246310005"
    }

    @Test
    fun `extracts 13-digit legacy PAN`() {
        val lines =
            listOf(
                RecognizedLine("4222 2222 2222 2", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("12/29", NormalisedRect(0f, 0.6f, 1f, 0.7f)),
            )
        parser.parse(lines, false)!!.pan shouldBe "4222222222222"
    }

    @Test
    fun `rejects non-ASCII digits in PAN`() {
        val lines =
            listOf(
                RecognizedLine("٤١١١ ١١١١ ١١١١ ١١١١", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("12/29", NormalisedRect(0f, 0.6f, 1f, 0.7f)),
            )
        parser.parse(lines, false) shouldBe null
    }

    @Test
    fun `does not match four-digit PAN-group as expiry via fallback`() {
        // Regex must require a separator — a PAN group like "1111" must not match.
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                // no expiry line — the PAN digits must NOT be interpreted as expiry
            )
        parser.parse(lines, false) shouldBe null
    }

    @Test
    fun `reads an expiry printed with a four-digit year`() {
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("VALID THRU 12/2029", belowPan),
            )
        val card = parser.parse(lines, false).shouldNotBeNull()
        card.expiryMonth shouldBe 12
        card.expiryYear shouldBe 2029
    }

    @Test
    fun `a four-digit year in the past is still rejected`() {
        val lines =
            listOf(
                RecognizedLine("4111 1111 1111 1111", NormalisedRect(0f, 0.3f, 1f, 0.4f)),
                RecognizedLine("03/2024", belowPan),
            )
        parser.parse(lines, false) shouldBe null
    }
}
