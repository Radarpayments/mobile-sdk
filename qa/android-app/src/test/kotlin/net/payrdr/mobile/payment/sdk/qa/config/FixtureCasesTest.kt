package net.payrdr.mobile.payment.sdk.qa.config

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File
import kotlin.test.Test

class FixtureCasesTest {
    private val cases: List<JsonObject> =
        (Json.parseToJsonElement(File("../fixtures/cases.json").readText()) as JsonArray).map { it.jsonObject }

    @Test
    fun `the fixture file has cases of both kinds`() {
        cases.map { it.getValue("kind").jsonPrimitive.content } shouldContain "config"
        cases.map { it.getValue("kind").jsonPrimitive.content } shouldContain "profile"
        (cases.size > 40) shouldBe true
    }

    @TestFactory
    fun `every fixture case decodes as the contract says`(): List<DynamicTest> =
        cases.map { case ->
            val name = case.getValue("name").jsonPrimitive.content
            DynamicTest.dynamicTest(name) { run(case) }
        }

    private fun run(case: JsonObject) {
        val text = case["inputText"]?.jsonPrimitive?.content ?: case.getValue("input").toString()
        val expect = case.getValue("expect").jsonObject
        val isConfig = case.getValue("kind").jsonPrimitive.content == "config"
        when (val decoded = if (isConfig) QaConfigJson.decode(text) else StandProfileJson.decode(text)) {
            is Decoded.Ok<*> -> {
                val document = expect["document"] ?: error("the case expects an error but the document was accepted")
                val written =
                    when (val value = decoded.value) {
                        is QaConfig -> QaConfigJson.encode(value)
                        is StandProfile -> StandProfileJson.encode(value)
                        else -> error("unexpected decoded type")
                    }
                Json.parseToJsonElement(written) shouldBe document
            }

            is Decoded.Invalid ->
                decoded.problem.field shouldBe
                    (expect["errorField"]?.jsonPrimitive?.content ?: error("the case expects a document"))
        }
    }
}
