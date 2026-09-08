package com.example.galeria.features.gallery.data

import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.withMediaMetadataRetriever
import com.example.galeria.features.gallery.domain.displaySize
import com.example.galeria.features.gallery.domain.parseEmbeddedVideoDate

internal data class EmbeddedVideoMetadata(
    val durationMs: Long = 0L,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val capturedAtEpochMs: Long = 0L,
    val title: String? = null,
)

internal fun readEmbeddedVideoMetadata(publicUri: String): EmbeddedVideoMetadata? {
    val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return null
    val uri = runCatching { publicUri.toUri() }.getOrNull() ?: return null
    return withMediaMetadataRetriever { retriever ->
        retriever.setDataSource(context, uri)
        val duration = retriever.metaLong(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val width = retriever.metaInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val height = retriever.metaInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val rotation = retriever.metaInt(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        val (displayWidth, displayHeight) = displaySize(width, height, rotation)
        val captured = parseEmbeddedVideoDate(
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE),
        )
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        EmbeddedVideoMetadata(
            durationMs = duration,
            widthPx = displayWidth,
            heightPx = displayHeight,
            capturedAtEpochMs = captured,
            title = title,
        )
    }
}

private fun MediaMetadataRetriever.metaLong(key: Int): Long =
    extractMetadata(key)?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L

private fun MediaMetadataRetriever.metaInt(key: Int): Int =
    extractMetadata(key)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
