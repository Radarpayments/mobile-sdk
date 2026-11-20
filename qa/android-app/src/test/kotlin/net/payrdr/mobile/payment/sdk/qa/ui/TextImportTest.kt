package net.payrdr.mobile.payment.sdk.qa.ui

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.intake.Intake
import net.payrdr.mobile.payment.sdk.qa.intake.intakeFromText
import kotlin.test.Test

class TextImportTest {
    private val configJson = """{"profile":"sandbox","order":{"amount":500}}"""
    private val profileJson = """{"name":"sandbox","gatewayUrl":"https://dev.bpcbt.com/payment","api":"v2","apiKey":"key-1"}"""
    private val accepted = mutableListOf<List<Intake>>()

    private fun typed(text: String) = TextImport().apply { edit(text) }

    @Test
    fun `import is off while the text is empty after trimming`() {
        typed("").canImport shouldBe false
        typed(" \r\n\t ").canImport shouldBe false
        typed("\uFEFF \n").canImport shouldBe false
        typed("{").canImport shouldBe true
        typed("\u00A0").canImport shouldBe true
    }

    @Test
    fun `blank text is not handed on`() {
        val import = typed("  \n")
        import.submit { accepted += it } shouldBe false
        accepted shouldBe emptyList()
        import.problem shouldBe null
    }

    @Test
    fun `a configuration is handed on as clipboard text is and the screen closes`() {
        val import = typed("\n  $configJson\n")
        import.submit { accepted += it } shouldBe true
        accepted shouldBe listOf(listOf(Intake.Config(QaConfig(profile = "sandbox", order = OrderConfig(amount = 500)))))
        import.problem shouldBe null
        import.text shouldBe ""
    }

    @Test
    fun `a profile is handed on to be confirmed`() {
        val import = typed(profileJson)
        import.submit { accepted += it } shouldBe true
        val profile = StandProfile("sandbox", "https://dev.bpcbt.com/payment", api = ApiVersion.V2, apiKey = "key-1")
        accepted shouldBe listOf(listOf(Intake.Profile(profile, needsConfirmation = true)))
    }

    @Test
    fun `a rejection keeps the text and says why`() {
        val text = """{"order":{"amount":"five"}}"""
        val import = typed(text)
        import.submit { accepted += it } shouldBe false
        val rejection = intakeFromText(text).single() as Intake.Rejected
        accepted shouldBe listOf(listOf<Intake>(rejection))
        import.problem shouldBe rejection.problem
        import.text shouldBe text
    }

    @Test
    fun `text that is neither json nor a link is rejected at the root`() {
        val import = typed("payrdr")
        import.submit { accepted += it } shouldBe false
        import.problem?.field shouldBe "$"
    }

    @Test
    fun `editing the text clears the problem`() {
        val import = typed("payrdr")
        import.submit { accepted += it }
        import.edit("payrdr{")
        import.problem shouldBe null
    }

    @Test
    fun `clearing drops the text and the problem`() {
        val import = typed("payrdr")
        import.submit { accepted += it }
        import.clear()
        import.text shouldBe ""
        import.problem shouldBe null
        import.canImport shouldBe false
    }
}
