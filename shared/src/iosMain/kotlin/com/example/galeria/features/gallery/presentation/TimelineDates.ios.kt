package com.example.galeria.features.gallery.presentation

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

actual fun daySectionFor(epochMs: Long): DaySection {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMs / 1000.0)
    val calendar = NSCalendar.currentCalendar
    val now = NSDate()
    val year = calendar.component(NSCalendarUnitYear, date)
    val month = calendar.component(platform.Foundation.NSCalendarUnitMonth, date)
    val day = calendar.component(platform.Foundation.NSCalendarUnitDay, date)
    val key = "$year-$month-$day"
    val label = when {
        calendar.isDateInToday(date) -> "Hoy"
        calendar.isDateInYesterday(date) -> "Ayer"
        else -> {
            val formatter = NSDateFormatter()
            formatter.locale = NSLocale.currentLocale
            val yearNow = calendar.component(NSCalendarUnitYear, now)
            formatter.dateFormat = if (year == yearNow) "d MMMM" else "d MMMM yyyy"
            formatter.stringFromDate(date)
        }
    }
    return DaySection(key = key, label = label)
}
