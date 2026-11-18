package net.payrdr.mobile.payment.sdk.qa.merchant

import net.payrdr.mobile.payment.sdk.qa.config.StandProfile

/**
 * The profile's password and API key in every spelling a gateway reply may quote them back in: an error page or a JSON
 * that echoes the request. Whatever leaves [Merchant] as text — a journal line, a failure message — goes through here.
 *
 * Each character of a secret is matched in any of its spellings independently (raw, `+` for a space, percent-encoded,
 * JSON-escaped, an HTML entity), as on iOS, so a mix, such as a form encoder that leaves some characters raw next to a
 * JSON writer that escapes others, is masked as well.
 */
internal class GatewaySecrets private constructor(
    private val secrets: List<IntArray>,
) {
    fun mask(text: String): String = masked(text, Int.MAX_VALUE).first

    /**
     * Masks first and cuts afterwards: a cut through the middle of a secret would leave its first half, which no
     * longer matches anything to mask. The limit counts UTF-16 units but never splits a surrogate pair: the character
     * that would be split is left out, as on iOS.
     */
    fun bounded(
        text: String,
        limit: Int,
        cutMarker: String = "",
    ): String {
        val (masked, cut) = masked(text, limit)
        return if (cut) masked + cutMarker else masked
    }

    override fun toString(): String = "GatewaySecrets(${secrets.size} secrets)"

    private fun masked(
        text: String,
        limit: Int,
    ): Pair<String, Boolean> {
        val input = text.codePoints().toArray()
        val output = StringBuilder()

        fun append(codePoint: Int): Boolean {
            if (output.length + Character.charCount(codePoint) > limit) return false
            output.appendCodePoint(codePoint)
            return true
        }
        var position = 0
        while (position < input.size) {
            val end = longestMatch(input, position)
            val pieces = if (end == null) intArrayOf(input[position]) else MASK_CODE_POINTS
            if (!pieces.all(::append)) return output.toString() to true
            position = end ?: (position + 1)
        }
        return output.toString() to false
    }

    private fun longestMatch(
        input: IntArray,
        start: Int,
    ): Int? = secrets.mapNotNull { matchEnd(it, input, start) }.maxOrNull()

    private fun matchEnd(
        secret: IntArray,
        input: IntArray,
        start: Int,
    ): Int? {
        var ends = setOf(start)
        for (codePoint in secret) {
            ends = ends.flatMapTo(HashSet()) { spellingEnds(codePoint, input, it) }
            if (ends.isEmpty()) return null
        }
        return ends.max()
    }

    companion object {
        const val MASK = "***"

        private val MASK_CODE_POINTS = MASK.codePoints().toArray()
        private val JSON_SHORT_ESCAPES =
            mapOf(
                '"'.code to '"',
                '\\'.code to '\\',
                '/'.code to '/',
                '\b'.code to 'b',
                0x0C to 'f',
                '\n'.code to 'n',
                '\r'.code to 'r',
                '\t'.code to 't',
            )
        private val HTML_NAMED_ENTITIES =
            mapOf('&'.code to "&amp;", '<'.code to "&lt;", '>'.code to "&gt;", '"'.code to "&quot;", '\''.code to "&apos;")
        private const val HEX_DIGITS = "0123456789abcdef"
        private const val MAX_CODE_POINT = 0x10FFFF
        private const val ENTITY_DIGITS = 9

        fun of(profile: StandProfile): GatewaySecrets =
            GatewaySecrets(
                listOf(profile.password, profile.apiKey)
                    .filter { it.isNotBlank() }
                    .map { it.codePoints().toArray() },
            )

        // Where one character of a secret ends when it starts at [start], in each spelling that fits there.
        private fun spellingEnds(
            codePoint: Int,
            input: IntArray,
            start: Int,
        ): List<Int> {
            if (start >= input.size) return emptyList()
            val escaped =
                when (input[start]) {
                    '+'.code -> if (codePoint == ' '.code) start + 1 else null
                    '%'.code -> percentEnd(codePoint, input, start)
                    '\\'.code -> jsonEscapeEnd(codePoint, input, start)
                    '&'.code -> htmlEntityEnd(codePoint, input, start)
                    else -> null
                }
            return listOfNotNull(if (input[start] == codePoint) start + 1 else null, escaped)
        }

        // %HH for each UTF-8 byte, either case of hex digit. A lone surrogate has no UTF-8 form.
        private fun percentEnd(
            codePoint: Int,
            input: IntArray,
            start: Int,
        ): Int? {
            if (codePoint in Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code) return null
            var position = start
            for (byte in String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8)) {
                if (escapedValue(input, position, "%", 2) != byte.toInt() and 0xFF) return null
                position += 3
            }
            return position
        }

        // \uHHHH (a surrogate pair beyond the basic plane), or the short escapes JSON writers use.
        private fun jsonEscapeEnd(
            codePoint: Int,
            input: IntArray,
            start: Int,
        ): Int? {
            val short = JSON_SHORT_ESCAPES[codePoint]
            if (short != null && start + 1 < input.size && input[start + 1] == short.code) return start + 2
            var position = start
            for (unit in Character.toChars(codePoint)) {
                if (escapedValue(input, position, "\\u", 4) != unit.code) return null
                position += 6
            }
            return position
        }

        // The entities an HTML page escapes with, and a numeric reference to any character.
        private fun htmlEntityEnd(
            codePoint: Int,
            input: IntArray,
            start: Int,
        ): Int? {
            val entity = HTML_NAMED_ENTITIES[codePoint]
            if (entity != null && matches(entity, input, start)) return start + entity.length
            if (start + 3 >= input.size || input[start + 1] != '#'.code) return null
            val hex = input[start + 2] == 'x'.code || input[start + 2] == 'X'.code
            val digitsStart = if (hex) start + 3 else start + 2
            val semicolon =
                (digitsStart until minOf(input.size, digitsStart + ENTITY_DIGITS))
                    .firstOrNull { input[it] == ';'.code }
                    ?.takeIf { it > digitsStart }
            val value = semicolon?.let { numberValue(input, digitsStart, it, radix = if (hex) 16 else 10) }
            return if (semicolon != null && value == codePoint) semicolon + 1 else null
        }

        // The hex value of [digits] digits after [prefix] at [start], or null when the text there is not that escape.
        private fun escapedValue(
            input: IntArray,
            start: Int,
            prefix: String,
            digits: Int,
        ): Int? {
            val digitsStart = start + prefix.length
            if (digitsStart + digits > input.size || !matches(prefix, input, start)) return null
            return numberValue(input, digitsStart, digitsStart + digits, radix = 16)
        }

        private fun matches(
            text: String,
            input: IntArray,
            start: Int,
        ): Boolean = start + text.length <= input.size && text.indices.all { input[start + it] == text[it].code }

        // ASCII digits only: Character.digit would also read full-width and other scripts' digits.
        private fun numberValue(
            input: IntArray,
            from: Int,
            until: Int,
            radix: Int,
        ): Int? {
            var value = 0
            for (index in from until until) {
                val char = input[index]
                val digit = if (char < 0x80) HEX_DIGITS.indexOf(Char(char).lowercaseChar()) else -1
                if (digit !in 0 until radix || value > MAX_CODE_POINT) return null
                value = value * radix + digit
            }
            return value
        }
    }
}
