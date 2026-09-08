package com.example.galeria.features.gallery.domain

internal fun looksLikeDownloadedVideo(relativePath: String?, bucketName: String?): Boolean {
    val haystack = listOfNotNull(relativePath, bucketName).joinToString(" ").lowercase()
    return "download" in haystack || "descarga" in haystack
}

internal fun videoNeedsFileMetadata(
    durationMs: Long,
    widthPx: Int,
    heightPx: Int,
    hasTakenDate: Boolean,
    relativePath: String?,
    bucketName: String?,
): Boolean {
    if (durationMs <= 0L || widthPx <= 0 || heightPx <= 0) return true
    if (hasTakenDate) return false
    return looksLikeDownloadedVideo(relativePath, bucketName)
}

internal fun parseEmbeddedVideoDate(raw: String?): Long {
    if (raw.isNullOrBlank()) return 0L
    val digits = raw.filter { it.isDigit() }
    if (digits.length < 8) return 0L
    val year = digits.substring(0, 4).toIntOrNull() ?: return 0L
    val month = digits.substring(4, 6).toIntOrNull() ?: return 0L
    val day = digits.substring(6, 8).toIntOrNull() ?: return 0L
    if (year !in 1970..2100 || month !in 1..12 || day !in 1..31) return 0L
    val hour = digits.sliceOrZero(8, 10).coerceIn(0, 23)
    val minute = digits.sliceOrZero(10, 12).coerceIn(0, 59)
    val second = digits.sliceOrZero(12, 14).coerceIn(0, 59)
    return utcEpochMs(year, month, day, hour, minute, second)
}

internal fun preferEmbeddedVideoTitle(fileName: String, embeddedTitle: String?): String {
    val title = embeddedTitle?.trim().orEmpty()
    if (title.isEmpty()) return fileName
    val stem = fileName.substringBeforeLast('.')
    if (title.equals(fileName, ignoreCase = true) || title.equals(stem, ignoreCase = true)) {
        return fileName
    }
    return title
}

private fun String.sliceOrZero(start: Int, end: Int): Int {
    if (length < end) return 0
    return substring(start, end).toIntOrNull() ?: 0
}

private fun utcEpochMs(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    second: Int,
): Long {
    val days = civilToUnixDays(year, month, day)
    return (days * 86_400L + hour * 3_600L + minute * 60L + second) * 1_000L
}

private fun civilToUnixDays(year: Int, month: Int, day: Int): Long {
    var y = year.toLong()
    val m = month.toLong()
    val d = day.toLong()
    y -= if (m <= 2) 1 else 0
    val era = if (y >= 0) y / 400 else (y - 399) / 400
    val yoe = y - era * 400
    val mp = (m + 9) % 12
    val doy = (153 * mp + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146_097 + doe - 719_468
}
