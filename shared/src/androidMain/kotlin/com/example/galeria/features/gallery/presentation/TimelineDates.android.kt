package com.example.galeria.features.gallery.presentation

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun daySectionFor(epochMs: Long): DaySection {
    val locale = Locale.getDefault()
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMs
    val now = Calendar.getInstance()
    val key = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val label = when {
        sameDay(cal, now) -> "Hoy"
        sameDay(cal, yesterday) -> "Ayer"
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) ->
            SimpleDateFormat("d MMMM", locale).format(Date(epochMs))
        else ->
            SimpleDateFormat("d MMMM yyyy", locale).format(Date(epochMs))
    }
    return DaySection(key = key, label = label)
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
