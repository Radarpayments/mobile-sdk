package net.payrdr.mobile.payment.sdk.qa.intake

import net.payrdr.mobile.payment.sdk.qa.config.Decoded
import net.payrdr.mobile.payment.sdk.qa.config.MAX_DOCUMENT_BYTES
import net.payrdr.mobile.payment.sdk.qa.config.NOT_AN_OBJECT
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.StandProfileJson
import net.payrdr.mobile.payment.sdk.qa.config.limitProblem
import net.payrdr.mobile.payment.sdk.qa.config.parseObject
import java.io.ByteArrayOutputStream
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
 * Reads a `payrdrqa://config?data=…[&launch=1]` or `payrdrqa://profile?data=…` link after [trimInput]; the scheme
 * ignores ASCII case, the host does not.
 * A config yields [Intake.Config], followed by [Intake.Launch] when `launch` is exactly `1`; a profile yields an
 * [Intake.Profile] that needs confirmation. Anything unreadable or invalid yields a single [Intake.Rejected].
 */
internal fun intakeFromLink(link: String): List<Intake> {
    val trimmed = trimInput(link)
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
 * Reads the launch extras `qaConfig` and `qaProfile` (raw JSON after [trimInput], null when absent; empty after trimming
 * is unreadable) and `qaLaunch`. The result applies the profile, unconfirmed, before the config and launches last. When
 * either document cannot be used the result is a single [Intake.Rejected] for the first of them, and nothing is applied
 * or launched.
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
 * Reads pasted or scanned text after [trimInput]: text starting with `payrdrqa://` in any ASCII case as [intakeFromLink]
 * does, otherwise a JSON object, which is a profile needing confirmation when it has a `gatewayUrl` key and a config when
 * it has not.
 */
internal fun intakeFromText(text: String): List<Intake> {
    val trimmed = trimInput(text)
    if (asciiLowercase(trimmed.take(LINK_PREFIX.length)) == LINK_PREFIX) return intakeFromLink(trimmed)
    limitProblem(trimmed)?.let { return rejected(Problem("$", it)) }
    val root = parseObject(trimmed) ?: return rejected(NOT_AN_OBJECT)
    return if (GATEWAY_URL_KEY in root) profileIntake(trimmed, needsConfirmation = true) else listOf(configIntake(trimmed))
}

private enum class LinkTarget { CONFIG, PROFILE }

/**
 * Everything between `://` and the query must be the host itself, so a user-info, a port (even an empty one), a path or
 * an escape in the host is not a link. [java.net.URI] is not used: it accepts all of those, and it would decode `%26`
 * in the query into a separator.
 */
private fun linkTarget(link: String): LinkTarget? {
    if ('#' in link || asciiLowercase(link.take(LINK_PREFIX.length)) != LINK_PREFIX) return null
    return when (link.substring(LINK_PREFIX.length).substringBefore('?')) {
        CONFIG_HOST -> LinkTarget.CONFIG
        PROFILE_HOST -> LinkTarget.PROFILE
        else -> null
    }
}

/**
 * Drops one leading U+FEFF (a byte order mark that editors and clipboards leave), then ASCII space, tab, CR and LF from
 * both ends. Other whitespace stays, so both platforms trim the same characters.
 */
internal fun trimInput(text: String): String = text.removePrefix(BYTE_ORDER_MARK).trim { it in TRIMMED }

// Kotlin's case-insensitive comparison also folds non-ASCII letters, which Swift does not.
private fun asciiLowercase(text: String): String = text.map { if (it in 'A'..'Z') it + ('a' - 'A') else it }.joinToString("")

/** Every value of every parameter; a parameter without `=` has an empty value, one whose name cannot be decoded is dropped. */
private fun parseQuery(query: String): Map<String, List<String?>> =
    query
        .split('&')
        .filter { it.isNotEmpty() }
        .mapNotNull { pair ->
            val name = percentDecode(pair.substringBefore('=')) ?: return@mapNotNull null
            val value = pair.substringAfter('=', missingDelimiterValue = "")
            // An escape is three characters for one, so a longer raw value cannot decode to an acceptable `data`.
            name to if (value.length > MAX_DATA_LENGTH * ESCAPE_LENGTH) null else percentDecode(value)
        }.groupBy({ it.first }, { it.second })

/**
 * The UTF-8 JSON text carried in `data`, or null when it is empty, longer than base64url of the largest document, not
 * base64url or not UTF-8.
 */
private fun decodeData(value: String?): String? {
    if (value.isNullOrEmpty() || value.length > MAX_DATA_LENGTH) return null
    val bytes =
        try {
            BASE64URL.decode(value)
        } catch (_: IllegalArgumentException) {
            return null
        }
    return strictUtf8(bytes)
}

/**
 * `+` stays a `+`: this is a URI query, not a form, and base64url never needs a space. Null on a malformed escape or a hex
 * digit outside ASCII.
 */
private fun percentDecode(value: String): String? {
    val bytes = ByteArrayOutputStream()
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char == '%') {
            if (index + ESCAPE_LENGTH > value.length) return null
            val high = hexDigit(value[index + 1]) ?: return null
            val low = hexDigit(value[index + 2]) ?: return null
            bytes.write(high * HEX_RADIX + low)
            index += ESCAPE_LENGTH
        } else {
            bytes.write(char.toString().toByteArray(Charsets.UTF_8))
            index++
        }
    }
    return strictUtf8(bytes.toByteArray())
}

// digitToIntOrNull also reads Unicode digits and fullwidth letters, such as U+0664 for 4.
private fun hexDigit(char: Char): Int? =
    when (char) {
        in '0'..'9', in 'a'..'f', in 'A'..'F' -> char.digitToInt(HEX_RADIX)
        else -> null
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
    val trimmed = trimInput(text)
    if (trimmed.isEmpty()) return Intake.Rejected(DATA_PROBLEM)
    return when (val decoded = QaConfigJson.decode(trimmed)) {
        is Decoded.Ok -> Intake.Config(decoded.value)
        is Decoded.Invalid -> Intake.Rejected(decoded.problem)
    }
}

private fun profileIntake(
    text: String,
    needsConfirmation: Boolean,
): List<Intake> {
    val trimmed = trimInput(text)
    if (trimmed.isEmpty()) return rejected(DATA_PROBLEM)
    return when (val decoded = StandProfileJson.decode(trimmed)) {
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
private const val BYTE_ORDER_MARK = "\uFEFF"
private const val TRIMMED = " \t\r\n"
private const val BASE64_GROUP_BYTES = 3
private const val BASE64_GROUP_CHARS = 4

// Padded base64url of MAX_DOCUMENT_BYTES: 87384 characters.
private const val MAX_DATA_LENGTH = (MAX_DOCUMENT_BYTES + BASE64_GROUP_BYTES - 1) / BASE64_GROUP_BYTES * BASE64_GROUP_CHARS

// The contract names base64url only, so the standard alphabet's '+' and '/' are rejected rather than guessed at.
private val BASE64URL = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

private val LINK_PROBLEM = Problem("link", "must be payrdrqa://config or payrdrqa://profile")
private val DATA_PROBLEM = Problem("data", "must be UTF-8 JSON, base64url-encoded in a link")
