package net.payrdr.mobile.payment.sdk.qa.config

/** The largest document, in bytes of UTF-8. */
internal const val MAX_DOCUMENT_BYTES = 65_536

/** The deepest nesting of objects and arrays in a document; the root object is level 1. */
internal const val MAX_DOCUMENT_DEPTH = 32

/**
 * Why [text] is over [maxBytes] bytes of UTF-8 or nests objects and arrays deeper than [maxDepth] levels, or null when it
 * is within both. Brackets inside strings do not count. The text is not echoed.
 *
 * kotlinx parses and prints JSON recursively: a few thousand levels overflow the stack on parsing, a few hundred later
 * on printing (storage, the gateway request), and an exported activity takes such a document from any app.
 */
internal fun limitProblem(
    text: String,
    maxBytes: Int = MAX_DOCUMENT_BYTES,
    maxDepth: Int = MAX_DOCUMENT_DEPTH,
): String? {
    var bytes = 0L
    var depth = 0
    var inString = false
    var escaped = false
    for (char in text) {
        bytes += utf8Length(char)
        if (bytes > maxBytes) return "must be at most $maxBytes bytes of UTF-8"
        when {
            escaped -> escaped = false
            inString && char == '\\' -> escaped = true
            char == '"' -> inString = !inString
            inString -> Unit
            char == '{' || char == '[' -> if (++depth > maxDepth) return "must nest at most $maxDepth levels deep"
            char == '}' || char == ']' -> depth--
        }
    }
    return null
}

// A surrogate pair is four bytes, two per half; a lone surrogate is not UTF-8 and fails the parser anyway.
private fun utf8Length(char: Char): Int =
    when {
        char.code < 0x80 -> 1
        char.code < 0x800 || char.isSurrogate() -> 2
        else -> 3
    }
