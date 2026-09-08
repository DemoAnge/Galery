package com.example.galeria.features.gallery.presentation

import com.example.galeria.features.gallery.domain.MediaItem

internal const val LIBRARY_DEBOUNCE_MS = 1_200L
internal const val LIBRARY_MAX_WAIT_MS = 4_000L

internal fun libraryRefreshDelayMs(
    elapsedBurstMs: Long,
    debounceMs: Long = LIBRARY_DEBOUNCE_MS,
    maxWaitMs: Long = LIBRARY_MAX_WAIT_MS,
): Long = if (elapsedBurstMs >= maxWaitMs) 0L else debounceMs

internal fun libraryFingerprint(items: List<MediaItem>): Long {
    var mix = items.size.toLong()
    items.forEach { item ->
        var hash = item.id.hashCode().toLong()
        hash = hash * 31 + item.capturedAtEpochMs
        hash = hash * 31 + item.sizeBytes
        hash = hash * 31 + item.widthPx
        hash = hash * 31 + item.heightPx
        hash = hash * 31 + item.durationMs
        mix = mix xor hash
    }
    return mix
}

internal fun viewingAfterLibraryChange(
    session: ViewerSession?,
    available: Map<String, MediaItem>,
): ViewerSession? {
    if (session == null) return null
    val remaining = session.items.mapNotNull { available[it.id] }
    if (remaining.isEmpty()) return null
    val currentId = session.item.id
    val found = remaining.indexOfFirst { it.id == currentId }
    val index = if (found >= 0) found else session.index.coerceIn(0, remaining.lastIndex)
    return session.copy(
        items = remaining,
        index = index,
        image = if (remaining[index].id == currentId) session.image else null,
    )
}
