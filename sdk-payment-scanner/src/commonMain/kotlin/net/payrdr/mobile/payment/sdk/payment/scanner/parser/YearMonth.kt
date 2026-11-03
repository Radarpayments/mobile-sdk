package net.payrdr.mobile.payment.sdk.payment.scanner.parser

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** Minimal (year, month) tuple — kotlinx-datetime has no portable `YearMonth`. */
internal data class YearMonth(
    val year: Int,
    val month: Int,
) : Comparable<YearMonth> {
    override fun compareTo(other: YearMonth): Int {
        val yr = year.compareTo(other.year)
        return if (yr != 0) yr else month.compareTo(other.month)
    }

    companion object {
        fun systemNow(): YearMonth {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            return YearMonth(now.year, now.month.number)
        }
    }
}
