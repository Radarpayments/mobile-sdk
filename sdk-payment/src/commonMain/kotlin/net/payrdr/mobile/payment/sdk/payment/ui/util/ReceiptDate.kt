package net.payrdr.mobile.payment.sdk.payment.ui.util

/** A payment's date and time as the gateway writes it: `22.10.2026 14:04:07` (seconds optional) or `2026-10-22 14:04:07`. */
internal data class GatewayDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
) {
    companion object {
        private val DAY_FIRST = Regex("""(\d{1,2})\.(\d{1,2})\.(\d{4})\s+(\d{1,2}):(\d{2})(?::\d{2})?""")
        private val YEAR_FIRST = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})[ T](\d{1,2}):(\d{2})(?::\d{2}(?:\.\d+)?)?""")

        /** null when [text] is not one of the gateway's forms or names no real time. */
        fun parse(text: String?): GatewayDateTime? {
            val raw = text?.trim().orEmpty()
            val parsed =
                DAY_FIRST.matchEntire(raw)?.groupValues?.let { g ->
                    dateTime(year = g[3], month = g[2], day = g[1], hour = g[4], minute = g[5])
                }
                    ?: YEAR_FIRST.matchEntire(raw)?.groupValues?.let { g ->
                        dateTime(year = g[1], month = g[2], day = g[3], hour = g[4], minute = g[5])
                    }
            return parsed?.takeIf { it.month in 1..12 && it.day in 1..31 && it.hour in 0..23 && it.minute in 0..59 }
        }

        private fun dateTime(
            year: String,
            month: String,
            day: String,
            hour: String,
            minute: String,
        ) = GatewayDateTime(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt())
    }
}

/**
 * [dateTime] as the receipt shows it in [language]: the day, the month by name and the time (`22 октября, 14:04`), with
 * the year only when it is not the current one; worded by the platform.
 */
internal expect fun receiptDateText(
    dateTime: GatewayDateTime,
    language: String,
): String
