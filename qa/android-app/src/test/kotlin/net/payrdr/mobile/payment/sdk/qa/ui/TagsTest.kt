package net.payrdr.mobile.payment.sdk.qa.ui

import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

class TagsTest {
    private val android =
        File("../fixtures/identifiers.txt")
            .readLines()
            .filter { it.isNotBlank() && !it.substringAfter('#', "").trim().equals("ios", ignoreCase = true) }
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }

    @Test
    fun `the tags are exactly the identifiers of the contract for android`() {
        val fixed = android.filterNot { '<' in it }
        Tags.all.toSet() shouldBe fixed.toSet()
        Tags.all.size shouldBe fixed.size
    }

    @Test
    fun `the tags that end in a value are exactly the patterns of the contract for android`() {
        val patterns = android.filter { '<' in it }
        Tags.patterns.toSet() shouldBe patterns.toSet()
        Tags.patterns.size shouldBe patterns.size
    }

    @Test
    fun `a profile row is tagged with its name`() {
        Tags.profileSelect("sandbox") shouldBe "qa_profile_select_sandbox"
        Tags.profileEdit("sandbox") shouldBe "qa_profile_edit_sandbox"
    }

    @Test
    fun `an option is tagged with its value in snake case`() {
        Tags.option(Tags.ORDER_CURRENCY, "EUR") shouldBe "qa_order_currency_eur"
        Tags.option(Tags.SDK_THEME, "largeText") shouldBe "qa_sdk_theme_large_text"
        Tags.option(Tags.ORDER_SOURCE, "mdOrder") shouldBe "qa_order_source_md_order"
        Tags.option(Tags.SDK_LOCALE, null) shouldBe "qa_sdk_locale_none"
        Tags.option(Tags.SDK_GOOGLE_PAY_ENVIRONMENT, "") shouldBe "qa_sdk_google_pay_environment_none"
        Tags.feature("FORCE_TDS") shouldBe "qa_order_feature_force_tds"
        Tags.option(Tags.SDK_GOOGLE_PAY_ALLOWED_AUTH_METHODS, "CRYPTOGRAM_3DS") shouldBe
            "qa_sdk_google_pay_allowed_auth_methods_cryptogram_3ds"
    }

    @Test
    fun `every config field tag is a tag of the contract`() {
        Tags.all.containsAll(Tags.fields) shouldBe true
    }
}
