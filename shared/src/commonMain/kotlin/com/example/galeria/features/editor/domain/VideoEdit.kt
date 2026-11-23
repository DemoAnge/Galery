package com.example.galeria.features.editor.domain

data class VideoEdit(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val rotationDegrees: Int = 0,
    val mute: Boolean = false,
)

fun VideoEdit.clamped(durationMs: Long): VideoEdit {
    val duration = durationMs.coerceAtLeast(0L)
    val rotation = normalizedRotation(rotationDegrees)
    if (duration == 0L) {
        return copy(startMs = 0L, endMs = 0L, rotationDegrees = rotation)
    }
    val minSpan = minOf(duration, MIN_TRIM_MS)
    val start = startMs.coerceIn(0L, (duration - minSpan).coerceAtLeast(0L))
    val requestedEnd = if (endMs <= 0L) duration else endMs
    val end = requestedEnd.coerceIn(start + minSpan, duration)
    return copy(startMs = start, endMs = end, rotationDegrees = rotation)
}

fun VideoEdit.isIdentity(durationMs: Long): Boolean {
    val clamped = clamped(durationMs)
    return clamped.startMs == 0L &&
        clamped.endMs == durationMs &&
        clamped.rotationDegrees == 0 &&
        !clamped.mute
}

fun VideoEdit.rotatedBy(deltaDegrees: Int): VideoEdit =
    copy(rotationDegrees = normalizedRotation(rotationDegrees + deltaDegrees))

fun normalizedRotation(degrees: Int): Int {
    val n = ((degrees % 360) + 360) % 360
    return when (n) {
        in 45 until 135 -> 90
        in 135 until 225 -> 180
        in 225 until 315 -> 270
        else -> 0
    }
}

const val MIN_TRIM_MS = 200L
