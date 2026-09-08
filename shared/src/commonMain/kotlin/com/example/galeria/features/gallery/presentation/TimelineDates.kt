package com.example.galeria.features.gallery.presentation

import com.example.galeria.features.gallery.domain.MediaItem

data class DaySection(
    val key: String,
    val label: String,
)

data class TimelineGroup(
    val key: String,
    val label: String,
    val items: List<MediaItem>,
)

expect fun daySectionFor(epochMs: Long): DaySection

fun groupByDay(items: List<MediaItem>): List<TimelineGroup> {
    if (items.isEmpty()) return emptyList()
    val ordered = items.sortedByDescending { it.capturedAtEpochMs }
    return ordered
        .groupBy { daySectionFor(it.capturedAtEpochMs) }
        .map { (section, list) -> TimelineGroup(section.key, section.label, list) }
}
