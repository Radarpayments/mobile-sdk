package net.payrdr.mobile.payment.sdk.qa.config

/**
 * Whether the whole of [text] is one JSON value as RFC 8259 spells it, with nothing but space, tab, line feed and carriage
 * return around it, and accepted exactly where the iOS app's parser accepts it. A `\u` escape of a surrogate must be half
 * of a pair, and so must a raw surrogate.
 *
 * kotlinx alone reads text that is not JSON (`01`, `+1`, `NaN`, a bare word, a raw tab in a string) and writes a bare
 * word back as it came, so a document from a link would launch on Android and be refused on iOS. The check walks the
 * text once with its own stack of open containers, so no depth overflows it.
 */
internal fun isStrictJson(text: String): Boolean = StrictJsonScanner(text).scan()

private class StrictJsonScanner(
    private val text: String,
) {
    private enum class Step { VALUE, AFTER_VALUE, FAILED }

    private var index = 0

    // True for an object, false for an array; the innermost container is last.
    private val open = ArrayDeque<Boolean>()

    fun scan(): Boolean {
        var step = Step.VALUE
        while (step == Step.VALUE || step == Step.AFTER_VALUE && open.isNotEmpty()) {
            step = if (step == Step.VALUE) value() else afterValue()
        }
        skipWhitespace()
        return step == Step.AFTER_VALUE && index == text.length
    }

    private fun value(): Step {
        skipWhitespace()
        val read =
            when (text.getOrNull(index)) {
                '{' -> return openContainer(isObject = true)
                '[' -> return openContainer(isObject = false)
                '"' -> string()
                't' -> literal("true")
                'f' -> literal("false")
                'n' -> literal("null")
                else -> number()
            }
        return if (read) Step.AFTER_VALUE else Step.FAILED
    }

    private fun openContainer(isObject: Boolean): Step {
        index++
        skipWhitespace()
        if (consume(if (isObject) '}' else ']')) return Step.AFTER_VALUE
        open.addLast(isObject)
        return if (!isObject || key()) Step.VALUE else Step.FAILED
    }

    private fun afterValue(): Step {
        skipWhitespace()
        val isObject = open.last()
        return when {
            consume(if (isObject) '}' else ']') -> {
                open.removeLast()
                Step.AFTER_VALUE
            }
            !consume(',') -> Step.FAILED
            !isObject || key() -> Step.VALUE
            else -> Step.FAILED
        }
    }

    private fun key(): Boolean {
        skipWhitespace()
        if (text.getOrNull(index) != '"' || !string()) return false
        skipWhitespace()
        return consume(':')
    }

    private fun string(): Boolean {
        index++
        while (index < text.length) {
            val char = text[index++]
            val read =
                when {
                    char == '"' -> return true
                    char == '\\' -> escape()
                    char < ' ' -> false
                    char.isHighSurrogate() -> text.getOrNull(index++)?.isLowSurrogate() == true
                    else -> !char.isLowSurrogate()
                }
            if (!read) return false
        }
        return false
    }

    private fun escape(): Boolean {
        val char = text.getOrNull(index++)
        return when {
            char == 'u' -> unicodeEscape()
            char != null -> char in SIMPLE_ESCAPES
            else -> false
        }
    }

    private fun unicodeEscape(): Boolean {
        val unit = hexUnit()
        return when {
            unit == null -> false
            unit.isHighSurrogate() -> consume('\\') && consume('u') && hexUnit()?.isLowSurrogate() == true
            else -> !unit.isLowSurrogate()
        }
    }

    private fun hexUnit(): Char? {
        if (index + HEX_DIGITS > text.length) return null
        var unit = 0
        repeat(HEX_DIGITS) {
            val digit = hexDigit(text[index++]) ?: return null
            unit = unit shl HEX_DIGIT_BITS or digit
        }
        return unit.toChar()
    }

    // ASCII only: Kotlin's digitToIntOrNull also takes Arabic-Indic and fullwidth digits.
    private fun hexDigit(char: Char): Int? =
        when (char) {
            in '0'..'9' -> char - '0'
            in 'a'..'f' -> char - 'a' + DECIMAL_DIGITS
            in 'A'..'F' -> char - 'A' + DECIMAL_DIGITS
            else -> null
        }

    private fun number(): Boolean {
        consume('-')
        if (!consume('0') && digits() == 0) return false
        if (consume('.') && digits() == 0) return false
        if (consume('e') || consume('E')) {
            if (!consume('+')) consume('-')
            return digits() > 0
        }
        return true
    }

    private fun digits(): Int {
        val start = index
        while (index < text.length && text[index] in '0'..'9') index++
        return index - start
    }

    private fun literal(word: String): Boolean {
        if (!text.startsWith(word, index)) return false
        index += word.length
        return true
    }

    private fun consume(char: Char): Boolean {
        if (text.getOrNull(index) != char) return false
        index++
        return true
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index] in WHITESPACE) index++
    }

    private companion object {
        const val SIMPLE_ESCAPES = "\"\\/bfnrt"
        const val WHITESPACE = " \t\n\r"
        const val HEX_DIGITS = 4
        const val HEX_DIGIT_BITS = 4
        const val DECIMAL_DIGITS = 10
    }
}
