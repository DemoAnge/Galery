package com.example.galeria.features.vault.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.withMediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

internal actual fun encodeVaultThumbnailJpeg(
    plaintext: ByteArray,
    isVideo: Boolean,
    displayName: String,
): ByteArray? {
    return try {
        val bitmap = if (isVideo || !looksLikeStillImage(plaintext)) {
            vaultVideoFrameBitmap(plaintext, displayName, VAULT_THUMB_MAX_EDGE)
        } else {
            sampledBitmapFromBytes(plaintext, VAULT_THUMB_MAX_EDGE)
        } ?: return null
        try {
            val out = ByteArrayOutputStream()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)) return null
            out.toByteArray()
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    } catch (_: Throwable) {
        null
    }
}

internal fun sampledBitmapFromBytes(bytes: ByteArray, maxEdge: Int): Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    val largest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    if (maxEdge > 0) {
        while (largest / sample > maxEdge) sample *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
    return scaleDownBitmap(decoded, maxEdge)
}

internal fun vaultVideoFrameBitmap(
    bytes: ByteArray,
    displayName: String,
    maxEdge: Int,
): Bitmap? {
    val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return null
    purgeVaultThumbTemps(context.cacheDir)
    val ext = videoThumbExtension(displayName)
    val tmp = File.createTempFile("vault-thumb", ext, context.cacheDir)
    return try {
        tmp.writeBytes(bytes)
        withMediaMetadataRetriever { retriever ->
            retriever.setDataSource(tmp.absolutePath)
            val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            frame?.let { scaleDownBitmap(it, maxEdge) }
        }
    } catch (_: Throwable) {
        null
    } finally {
        tmp.delete()
    }
}

internal fun purgeVaultThumbTemps(cacheDir: File) {
    cacheDir.listFiles { file ->
        file.isFile && file.name.startsWith("vault-thumb")
    }?.forEach { leftover -> leftover.delete() }
}

private fun scaleDownBitmap(bitmap: Bitmap, maxEdge: Int): Bitmap {
    if (maxEdge <= 0) return bitmap
    val largest = max(bitmap.width, bitmap.height).coerceAtLeast(1)
    if (largest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / largest
    val scaled = Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true,
    )
    if (scaled !== bitmap && !bitmap.isRecycled) bitmap.recycle()
    return scaled
}

private fun videoThumbExtension(displayName: String): String {
    val name = displayName.lowercase()
    return when {
        name.endsWith(".mov") -> ".mov"
        name.endsWith(".webm") -> ".webm"
        name.endsWith(".mkv") -> ".mkv"
        name.endsWith(".3gp") -> ".3gp"
        name.endsWith(".m4v") -> ".m4v"
        else -> ".mp4"
    }
}
