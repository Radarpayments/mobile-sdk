package net.payrdr.mobile.payment.sdk.payment.presentation

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PaymentLanguageTest {
    @Test fun `language of a plain tag`() {
        resolveLanguage("de", systemLanguage = "en") shouldBe "de"
    }

    @Test fun `language of a tag with a region in either spelling and case`() {
        resolveLanguage("ar-AE", systemLanguage = "en") shouldBe "ar"
        resolveLanguage(" PT_br ", systemLanguage = "en") shouldBe "pt"
    }

    @Test fun `null or blank tag follows the system`() {
        resolveLanguage(null, systemLanguage = "ru") shouldBe "ru"
        resolveLanguage("  ", systemLanguage = "fr-CA") shouldBe "fr"
    }

    @Test fun `nothing to go by is English`() {
        resolveLanguage(null, systemLanguage = "") shouldBe "en"
    }

    @Test fun `right to left languages`() {
        isRtl("ar") shouldBe true
        isRtl("he") shouldBe true
        isRtl("en") shouldBe false
    }
}
