package net.payrdr.mobile.payment.sdk.payment.ui.util

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal actual fun receiptDateText(
    dateTime: GatewayDateTime,
    language: String,
): String {
    val locale = Locale(language)
    val calendar =
        Calendar.getInstance().apply {
            clear()
            set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute)
        }
    val thisYear = Calendar.getInstance().get(Calendar.YEAR) == dateTime.year
    val pattern = DateFormat.getBestDateTimePattern(locale, if (thisYear) "dMMMMHHmm" else "dMMMMyyyyHHmm")
    return SimpleDateFormat(pattern, locale).format(calendar.time)
}
