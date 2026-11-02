package net.payrdr.mobile.payment.sdk.payment.scanner.parser

import net.payrdr.mobile.payment.sdk.core.validation.rules.LuhnStringRule
import net.payrdr.mobile.payment.sdk.payment.scanner.ScannedCard
import net.payrdr.mobile.payment.sdk.payment.scanner.camera.RecognizedLine

/**
 * Extracts [ScannedCard] from OCR output. Pure Kotlin, no platform deps.
 *
 * Algorithm:
 *  1. collect every digit-run 13..19 long (stripping single spaces/hyphens), keep those whose
 *     Luhn passes, pick the longest; if none — return null.
 *  2. find an MM/YY (or MM-YY, MM YY) on the line below the PAN; reject if in the past.
 *  3. if [extractCardholder], pick the best uppercase-latin candidate not intersecting PAN/expiry
 *     and not in the issuer dictionary.
 */
public class CardParser(
    private val now: YearMonth = YearMonth.systemNow(),
) {
    private val luhn = LuhnStringRule(code = "LUHN", message = "luhn")

    public fun parse(
        lines: List<RecognizedLine>,
        extractCardholder: Boolean,
    ): ScannedCard? {
        val panMatch = findPan(lines) ?: return null
        val expiry = findExpiry(lines, panMatch) ?: return null
        val cardholder = if (extractCardholder) findCardholder(lines, panMatch, expiry) else null
        return ScannedCard(panMatch.digits, expiry.month, expiry.year, cardholder)
    }

    private fun findPan(lines: List<RecognizedLine>): PanMatch? {
        val candidates = mutableListOf<PanMatch>()
        for (line in lines) {
            val digits = line.text.filter { it in '0'..'9' }
            if (digits.length in 13..19 && luhn.validateForError(digits).isValid) {
                candidates += PanMatch(digits, line)
            }
        }
        return candidates.maxByOrNull { it.digits.length }
    }

    private fun findExpiry(
        lines: List<RecognizedLine>,
        pan: PanMatch,
    ): ExpiryMatch? {
        // MM/YY, and MM/YYYY as some issuers print it.
        val regex = Regex("\\b(0[1-9]|1[0-2])[\\/\\-\\s](20\\d{2}|\\d{2})\\b")
        val below =
            lines
                .filter { it !== pan.line && it.boundingBox.top > pan.line.boundingBox.top + 0.01f }
                .sortedBy { it.boundingBox.top }
        for (line in below + listOf(pan.line)) {
            val match = regex.find(line.text) ?: continue
            val month = match.groupValues[1].toInt()
            val yy = match.groupValues[2]
            val year = if (yy.length == 4) yy.toInt() else 2000 + yy.toInt()
            if (YearMonth(year, month) >= now) return ExpiryMatch(month, year, line)
        }
        return null
    }

    private fun findCardholder(
        lines: List<RecognizedLine>,
        pan: PanMatch,
        expiry: ExpiryMatch,
    ): String? {
        val blocked =
            setOf(
                "VISA",
                "MASTERCARD",
                "MAESTRO",
                "AMERICAN",
                "EXPRESS",
                "DEBIT",
                "CREDIT",
                "VALID",
                "THRU",
                "FROM",
                "MONTH",
                "YEAR",
                "CVC",
                "CVV",
            )
        val taken = setOf(pan.line, expiry.line)
        for (line in lines) {
            if (line in taken) continue
            val words = line.text.trim().split(Regex("\\s+"))
            if (words.size !in 2..4) continue
            if (words.any { it.length !in 2..26 || it.any { ch -> !ch.isLetter() } || it.any { ch -> ch.isLowerCase() } }) continue
            if (words.any { it.uppercase() in blocked }) continue
            return words.joinToString(" ")
        }
        return null
    }

    private data class PanMatch(
        val digits: String,
        val line: RecognizedLine,
    )

    private data class ExpiryMatch(
        val month: Int,
        val year: Int,
        val line: RecognizedLine,
    )
}
