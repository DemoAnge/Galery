package com.example.galeria.features.gallery.domain

import com.example.galeria.core.CoreMediaKind
import com.example.galeria.core.PublicMediaRef

enum class GalleryMediaKind {
    IMAGE,
    VIDEO,
}

/**
 * Medio público del sistema. [publicUri] no se incluye en [toString].
 */
data class MediaItem(
    val id: String,
    val publicUri: String,
    val displayName: String,
    val mediaKind: GalleryMediaKind,
    val capturedAtEpochMs: Long,
    val sizeBytes: Long,
    val relativePath: String? = null,
    val bucketName: String? = null,
    val ownerPackage: String? = null,
    val albumId: AlbumId = AlbumId.OTHER,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val durationMs: Long = 0L,
) {
    override fun toString(): String =
        "MediaItem(displayName=$displayName, mediaKind=$mediaKind, albumId=$albumId)"

    fun toPublicMediaRef(): PublicMediaRef = PublicMediaRef(
        publicUri = publicUri,
        displayName = displayName,
        mediaKind = when (mediaKind) {
            GalleryMediaKind.IMAGE -> CoreMediaKind.IMAGE
            GalleryMediaKind.VIDEO -> CoreMediaKind.VIDEO
        },
        capturedAtEpochMs = capturedAtEpochMs,
        sizeBytes = sizeBytes,
        albumLabel = albumId.title(),
    )
}

fun displaySize(widthPx: Int, heightPx: Int, orientationDegrees: Int): Pair<Int, Int> {
    val angle = ((orientationDegrees % 360) + 360) % 360
    return if (angle == 90 || angle == 270) {
        heightPx to widthPx
    } else {
        widthPx to heightPx
    }
}

fun videoFrameAspect(widthPx: Int, heightPx: Int): Float {
    if (widthPx <= 0 || heightPx <= 0) return 9f / 16f
    return (widthPx.toFloat() / heightPx.toFloat()).coerceIn(0.42f, 1.95f)
}
