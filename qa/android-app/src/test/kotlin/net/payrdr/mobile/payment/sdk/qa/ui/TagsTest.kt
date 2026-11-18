package net.payrdr.mobile.payment.sdk.qa.ui

import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

class TagsTest {
    @Test
    fun `the tags are exactly the identifiers of the contract for android`() {
        val android =
            File("../fixtures/identifiers.txt")
                .readLines()
                .filter { it.isNotBlank() && !it.substringAfter('#', "").trim().equals("ios", ignoreCase = true) }
                .map { it.substringBefore('#').trim() }
                .filter { it.isNotEmpty() && it != "qa_profile_select_<name>" }
        Tags.all.toSet() shouldBe android.toSet()
        Tags.all.size shouldBe android.size
    }

    @Test
    fun `a profile row is tagged with its name`() {
        Tags.profileSelect("sandbox") shouldBe "qa_profile_select_sandbox"
    }

    @Test
    fun `every config field tag is a tag of the contract`() {
        Tags.all.containsAll(Tags.fields) shouldBe true
    }
}
