package com.example.galeria.core

import android.media.MediaMetadataRetriever

internal val mediaMetadataLock = Any()

internal inline fun <T> withMediaMetadataRetriever(
    block: (MediaMetadataRetriever) -> T,
): T? {
    synchronized(mediaMetadataLock) {
        val retriever = MediaMetadataRetriever()
        return try {
            block(retriever)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
