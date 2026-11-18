package net.payrdr.mobile.payment.sdk.qa.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object StandProfileJson {
    private val fields = listOf("version", "name", "gatewayUrl", "api", "login", "password", "apiKey")
    private val writer = Json { prettyPrint = true }

    fun decode(text: String): Decoded<StandProfile> {
        val root = parseObject(text) ?: return Decoded.Invalid(NOT_AN_OBJECT)
        val reader = JsonReader()
        val version = reader.integral(root, "version", "version")
        if (version != null && version != 1L) reader.fail("version", "must be 1")
        val name = reader.text(root, "name", "name")
        if (name == null) reader.fail("name", "is required")
        val gatewayUrl = reader.text(root, "gatewayUrl", "gatewayUrl")
        if (gatewayUrl == null) reader.fail("gatewayUrl", "is required")
        val profile =
            StandProfile(
                name = name.orEmpty(),
                gatewayUrl = gatewayUrl.orEmpty(),
                api = reader.choice(root, "api", "api", ApiVersion.entries, { it.json }, ApiVersion.V1),
                login = reader.text(root, "login", "login").orEmpty(),
                password = reader.text(root, "password", "password").orEmpty(),
                apiKey = reader.text(root, "apiKey", "apiKey").orEmpty(),
            )
        val problem = firstInOrder(reader.problems + listOfNotNull(problemOf(profile)), fields)
        return if (problem == null) Decoded.Ok(profile) else Decoded.Invalid(problem)
    }

    fun encode(profile: StandProfile): String {
        val document: JsonElement =
            buildJsonObject {
                put("version", 1)
                put("name", profile.name)
                put("gatewayUrl", profile.gatewayUrl)
                put("api", profile.api.json)
                put("login", profile.login)
                put("password", profile.password)
                put("apiKey", profile.apiKey)
            }
        return writer.encodeToString(JsonElement.serializer(), document)
    }

    fun problemOf(profile: StandProfile): Problem? =
        when {
            !NAME_PATTERN.matches(profile.name) -> Problem("name", "must be 1 to 32 characters of a-z, 0-9, _ and -")
            !isHttpUrl(profile.gatewayUrl) -> Problem("gatewayUrl", "must be an absolute http or https URL")
            else -> null
        }
}

internal val NAME_PATTERN = Regex("[a-z0-9_-]{1,32}")
