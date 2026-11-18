package net.payrdr.mobile.payment.sdk.qa.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import java.math.BigDecimal

internal data class Problem(
    val field: String,
    val message: String,
)

internal sealed interface Decoded<out T> {
    data class Ok<T>(
        val value: T,
    ) : Decoded<T>

    data class Invalid(
        val problem: Problem,
    ) : Decoded<Nothing>
}

// WHITE_SPACE is Unicode's White_Space, the set Swift's whitespace uses, so both apps judge the same links. It is
// spelled out because Android's regex engine rejects UNICODE_CHARACTER_CLASS and its own \s is a different set. The
// scheme is spelled out in both cases because CASE_INSENSITIVE would also fold non-ASCII letters (U+017F into "s").
private const val WHITE_SPACE = "\\u0009-\\u000D\\u0020\\u0085\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000"
private val URL_PATTERN = Regex("^[hH][tT][tT][pP]([sS])?://[^$WHITE_SPACE/?#]+[^$WHITE_SPACE]*$")

// The most digits an accepted integral number can have: amounts end at 999999999999.
private const val MAX_INTEGER_DIGITS = 13

internal val NOT_AN_OBJECT = Problem("$", "the document is not a JSON object")

internal fun isHttpUrl(text: String): Boolean = URL_PATTERN.matches(text)

/**
 * The root object of [text], or null when the text is not RFC 8259 JSON (see [isStrictJson]) or not an object.
 * The parser's own message quotes the input, so it is dropped.
 */
internal fun parseObject(text: String): JsonObject? =
    try {
        if (isStrictJson(text)) Json.parseToJsonElement(text) as? JsonObject else null
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

/** The first problem of [problems] in the order of [fields]; a field that is not listed comes last. */
internal fun firstInOrder(
    problems: List<Problem>,
    fields: List<String>,
): Problem? =
    problems.minByOrNull { problem ->
        fields.indexOf(problem.field).let { if (it < 0) Int.MAX_VALUE else it }
    }

/**
 * Typed reads over a [JsonObject]. A value of the wrong JSON type is recorded as a problem named by its path and read as
 * absent, so the caller goes on with the default and finds every type problem, not just the first.
 */
internal class JsonReader {
    val problems = mutableListOf<Problem>()

    fun fail(
        field: String,
        message: String,
    ) {
        problems += Problem(field, message)
    }

    private fun present(
        source: JsonObject,
        key: String,
    ): JsonElement? = source[key]?.takeUnless { it is JsonNull }

    fun group(
        source: JsonObject,
        key: String,
        path: String,
    ): JsonObject {
        val element = present(source, key) ?: return JsonObject(emptyMap())
        if (element is JsonObject) return element
        fail(path, "must be an object")
        return JsonObject(emptyMap())
    }

    fun text(
        source: JsonObject,
        key: String,
        path: String,
    ): String? {
        val element = present(source, key) ?: return null
        if (element is JsonPrimitive && element.isString) return element.content
        fail(path, "must be a string")
        return null
    }

    fun nullableText(
        source: JsonObject,
        key: String,
        path: String,
    ): String? = text(source, key, path)?.takeIf { it.isNotBlank() }

    fun flag(
        source: JsonObject,
        key: String,
        path: String,
        default: Boolean,
    ): Boolean {
        val element = present(source, key) ?: return default
        val value = (element as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
        if (value == null) fail(path, "must be true or false")
        return value ?: default
    }

    /**
     * An integral JSON number by value (`100`, `100.0` and `1e2` are all 100). A number beyond [MAX_INTEGER_DIGITS] digits
     * is returned as the largest or smallest Long without being expanded, so an exponent like `1e999999999` costs nothing
     * and fails the range check of the caller.
     */
    fun integral(
        source: JsonObject,
        key: String,
        path: String,
    ): Long? {
        val element = present(source, key) ?: return null
        val number = (element as? JsonPrimitive)?.takeUnless { it.isString || it.booleanOrNull != null }
        val value = number?.let { integralValue(it.content) }
        if (value == null) fail(path, "must be an integer")
        return value
    }

    fun names(
        source: JsonObject,
        key: String,
        path: String,
    ): List<String>? {
        val element = present(source, key) ?: return null
        val items = (element as? JsonArray)?.map { (it as? JsonPrimitive)?.takeIf { item -> item.isString }?.content }
        if (items == null || items.any { it == null }) {
            fail(path, "must be a list of strings")
            return null
        }
        return items.filterNotNull()
    }

    fun <E> choice(
        source: JsonObject,
        key: String,
        path: String,
        values: List<E>,
        json: (E) -> String,
        default: E,
    ): E {
        val name = text(source, key, path) ?: return default
        val value = values.firstOrNull { json(it) == name }
        if (value == null) fail(path, "must be one of ${values.joinToString(", ", transform = json)}")
        return value ?: default
    }

    private fun integralValue(content: String): Long? {
        val number =
            try {
                BigDecimal(content)
            } catch (_: NumberFormatException) {
                return null
            }
        if (number.signum() == 0) return 0
        // Long arithmetic: an exponent near Int.MAX_VALUE overflows Int and would slip past the check into a huge expansion.
        val integerDigits = number.precision().toLong() - number.scale()
        if (integerDigits > MAX_INTEGER_DIGITS) return if (number.signum() < 0) Long.MIN_VALUE else Long.MAX_VALUE
        val stripped = number.stripTrailingZeros()
        return if (stripped.scale() > 0) null else stripped.toBigIntegerExact().toLong()
    }
}
