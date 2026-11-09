package net.payrdr.mobile.payment.sdk.payment.ui.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

internal actual fun receiptDateText(
    dateTime: GatewayDateTime,
    language: String,
): String {
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
    val components =
        NSDateComponents().apply {
            year = dateTime.year.toLong()
            month = dateTime.month.toLong()
            day = dateTime.day.toLong()
            hour = dateTime.hour.toLong()
            minute = dateTime.minute.toLong()
        }
    val date = calendar.dateFromComponents(components) ?: return "${dateTime.day}.${dateTime.month}.${dateTime.year}"
    val thisYear = calendar.component(NSCalendarUnitYear, fromDate = NSDate()) == dateTime.year.toLong()
    val formatter =
        NSDateFormatter().apply {
            locale = NSLocale(localeIdentifier = language)
            setCalendar(calendar)
            setLocalizedDateFormatFromTemplate(if (thisYear) "dMMMMHHmm" else "dMMMMyyyyHHmm")
        }
    return formatter.stringFromDate(date)
}
