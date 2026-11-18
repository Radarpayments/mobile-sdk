package net.payrdr.mobile.payment.sdk.qa.intake

import net.payrdr.mobile.payment.sdk.qa.config.Decoded
import net.payrdr.mobile.payment.sdk.qa.config.NOT_AN_OBJECT
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.StandProfileJson
import net.payrdr.mobile.payment.sdk.qa.config.parseObject
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URISyntaxException
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import kotlin.io.encoding.Base64

/** One step that external input asks for, applied in list order. */
internal sealed interface Intake {
    /** A valid configuration to apply at once. */
    data class Config(
        val config: QaConfig,
    ) : Intake

    /** A valid stand profile; with [needsConfirmation] it is saved only after the tester confirms it. */
    data class Profile(
        val profile: StandProfile,
        val needsConfirmation: Boolean,
    ) : Intake

    /** Start a run with the configuration in force after the preceding steps. */
    data object Launch : Intake

    /** The input cannot be used; nothing else comes with it, so nothing is applied. */
    data class Rejected(
        val problem: Problem,
    ) : Intake
}

/**
 * Reads a `payrdrqa://config?data=…[&launch=1]` or `payrdrqa://profile?data=…` link, surrounding whitespace allowed.
 * A config yields [Intake.Config], followed by [Intake.Launch] when `launch` is exactly `1`; a profile yields an
 * [Intake.Profile] that needs confirmation. Anything unreadable or invalid yields a single [Intake.Rejected].
 */
internal fun intakeFromLink(link: String): List<Intake> {
    val trimmed = link.trim()
    val target = linkTarget(trimmed) ?: return rejected(LINK_PROBLEM)
    val query = parseQuery(trimmed.substringAfter('?', missingDelimiterValue = ""))
    val text = query[DATA_PARAMETER]?.singleOrNull()?.let(::decodeData) ?: return rejected(DATA_PROBLEM)
    return when (target) {
        LinkTarget.CONFIG ->
            when (val decoded = QaConfigJson.decode(text)) {
                is Decoded.Ok ->
                    listOf(Intake.Config(decoded.value)) +
                        if (query[LAUNCH_PARAMETER] == listOf(LAUNCH_VALUE)) listOf(Intake.Launch) else emptyList()
                is Decoded.Invalid -> rejected(decoded.problem)
            }
        LinkTarget.PROFILE -> profileIntake(text, needsConfirmation = true)
    }
}

/**
 * Reads the launch extras `qaConfig` and `qaProfile` (raw JSON, null when absent) and `qaLaunch`. The result applies the
 * profile, unconfirmed, before the config and launches last. When either document cannot be used the result is a single
 * [Intake.Rejected] for the first of them, and nothing is applied or launched.
 */
internal fun intakeFromExtras(
    qaConfig: String?,
    qaProfile: String?,
    qaLaunch: Boolean,
): List<Intake> {
    val steps = mutableListOf<Intake>()
    if (qaProfile != null) {
        val profile = profileIntake(qaProfile, needsConfirmation = false).single()
        if (profile is Intake.Rejected) return listOf(profile)
        steps += profile
    }
    if (qaConfig != null) {
        val config = configIntake(qaConfig)
        if (config is Intake.Rejected) return listOf(config)
        steps += config
    }
    if (qaLaunch) steps += Intake.Launch
    return steps
}

/**
 * Reads pasted or scanned text after trimming it: a `payrdrqa://` link as [intakeFromLink] does, otherwise a JSON object,
 * which is a profile needing confirmation when it has a `gatewayUrl` key and a config when it has not.
 */
internal fun intakeFromText(text: String): List<Intake> {
    val trimmed = text.trim()
    if (trimmed.startsWith(LINK_PREFIX)) return intakeFromLink(trimmed)
    val root = parseObject(trimmed) ?: return rejected(NOT_AN_OBJECT)
    return if (GATEWAY_URL_KEY in root) profileIntake(trimmed, needsConfirmation = true) else listOf(configIntake(trimmed))
}

private enum class LinkTarget { CONFIG, PROFILE }

/**
 * The query is cut off by hand rather than read from [URI], which would decode `%26` into a separator and lose the
 * difference between `+` and its escape.
 */
private fun linkTarget(link: String): LinkTarget? {
    if ('#' in link) return null
    val uri =
        try {
            URI(link.substringBefore('?'))
        } catch (_: URISyntaxException) {
            return null
        }
    val plain =
        uri.scheme == SCHEME && uri.rawUserInfo == null && uri.port == -1 && uri.rawPath.isNullOrEmpty() && uri.rawQuery == null
    return when {
        !plain -> null
        uri.host == CONFIG_HOST -> LinkTarget.CONFIG
        uri.host == PROFILE_HOST -> LinkTarget.PROFILE
        else -> null
    }
}

/** Every value of every parameter; a parameter without `=` has an empty value, one whose name cannot be decoded is dropped. */
private fun parseQuery(query: String): Map<String, List<String?>> =
    query
        .split('&')
        .filter { it.isNotEmpty() }
        .mapNotNull { pair ->
            val name = percentDecode(pair.substringBefore('=')) ?: return@mapNotNull null
            name to percentDecode(pair.substringAfter('=', missingDelimiterValue = ""))
        }.groupBy({ it.first }, { it.second })

/** The UTF-8 JSON text carried in `data`, or null when it is empty, not base64url or not UTF-8. */
private fun decodeData(value: String?): String? {
    if (value.isNullOrEmpty()) return null
    val bytes =
        try {
            BASE64URL.decode(value)
        } catch (_: IllegalArgumentException) {
            return null
        }
    return strictUtf8(bytes)
}

/** `+` stays a `+`: this is a URI query, not a form, and base64url never needs a space. Null on a malformed escape. */
private fun percentDecode(value: String): String? {
    val bytes = ByteArrayOutputStream()
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char == '%') {
            if (index + ESCAPE_LENGTH > value.length) return null
            val high = value[index + 1].digitToIntOrNull(HEX_RADIX) ?: return null
            val low = value[index + 2].digitToIntOrNull(HEX_RADIX) ?: return null
            bytes.write(high * HEX_RADIX + low)
            index += ESCAPE_LENGTH
        } else {
            bytes.write(char.toString().toByteArray(Charsets.UTF_8))
            index++
        }
    }
    return strictUtf8(bytes.toByteArray())
}

/** [String] constructors replace malformed bytes with U+FFFD; a coder set to report them lets the caller reject instead. */
private fun strictUtf8(bytes: ByteArray): String? =
    try {
        Charsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }

private fun configIntake(text: String): Intake {
    if (text.isBlank()) return Intake.Rejected(DATA_PROBLEM)
    return when (val decoded = QaConfigJson.decode(text)) {
        is Decoded.Ok -> Intake.Config(decoded.value)
        is Decoded.Invalid -> Intake.Rejected(decoded.problem)
    }
}

private fun profileIntake(
    text: String,
    needsConfirmation: Boolean,
): List<Intake> {
    if (text.isBlank()) return rejected(DATA_PROBLEM)
    return when (val decoded = StandProfileJson.decode(text)) {
        is Decoded.Ok -> listOf(Intake.Profile(decoded.value, needsConfirmation))
        is Decoded.Invalid -> rejected(decoded.problem)
    }
}

private fun rejected(problem: Problem): List<Intake> = listOf(Intake.Rejected(problem))

private const val SCHEME = "payrdrqa"
private const val LINK_PREFIX = "$SCHEME://"
private const val CONFIG_HOST = "config"
private const val PROFILE_HOST = "profile"
private const val DATA_PARAMETER = "data"
private const val LAUNCH_PARAMETER = "launch"
private const val LAUNCH_VALUE = "1"
private const val GATEWAY_URL_KEY = "gatewayUrl"
private const val HEX_RADIX = 16
private const val ESCAPE_LENGTH = 3

// The contract names base64url only, so the standard alphabet's '+' and '/' are rejected rather than guessed at.
private val BASE64URL = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

private val LINK_PROBLEM = Problem("link", "must be payrdrqa://config or payrdrqa://profile")
private val DATA_PROBLEM = Problem("data", "must be UTF-8 JSON, base64url-encoded in a link")
