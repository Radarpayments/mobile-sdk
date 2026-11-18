package net.payrdr.mobile.payment.sdk.qa.config

import kotlinx.serialization.json.JsonObject

internal fun namesToText(names: List<String>): String = names.joinToString(",")

internal fun namesFromText(text: String): List<String> = text.split(',').map { it.trim() }.filter { it.isNotEmpty() }

internal fun pinsToText(pins: List<Pin>): String = pins.joinToString("\n") { "${it.host}=${it.pin}" }

// A pin ends in '=', so a line is split at the first one: a host has none.
internal fun pinsFromText(text: String): Decoded<List<Pin>> {
    val pins =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
            Pin(line.substringBefore('='), if ('=' in line) line.substringAfter('=') else "")
        }
    val problem = pinsProblem(pins)
    return if (problem == null) Decoded.Ok(pins) else Decoded.Invalid(problem)
}

internal fun extraToText(extra: JsonObject): String = extra.toString()

internal fun extraFromText(text: String): Decoded<JsonObject> {
    if (text.isBlank()) return Decoded.Ok(JsonObject(emptyMap()))
    val extra = parseObject(text) ?: return Decoded.Invalid(Problem("order.extra", "must be a JSON object"))
    val problem = extraProblem(extra)
    return if (problem == null) Decoded.Ok(extra) else Decoded.Invalid(problem)
}
