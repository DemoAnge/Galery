package com.example.galeria.features.vault.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.withMediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

internal actual fun encodeVaultThumbnailFromPublicUri(
    publicUri: String,
    isVideo: Boolean,
    displayName: String,
): ByteArray? {
    return try {
        val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return null
        val uri = publicUri.toUri()
        val bitmap = if (isVideo) {
            publicVideoFrame(context, uri)
        } else {
            publicStillBitmap(context, uri)
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

private fun publicVideoFrame(context: android.content.Context, uri: android.net.Uri): Bitmap? {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        runCatching {
            context.contentResolver.loadThumbnail(
                uri,
                android.util.Size(VAULT_THUMB_MAX_EDGE, VAULT_THUMB_MAX_EDGE),
                null,
            )
        }.getOrNull()?.let { return it }
    }
    return withMediaMetadataRetriever { retriever ->
        retriever.setDataSource(context, uri)
        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime
        frame?.let { scaleDownBitmap(it, VAULT_THUMB_MAX_EDGE) }
    }
}

private fun publicStillBitmap(context: android.content.Context, uri: android.net.Uri): Bitmap? {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
        return android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            val largest = max(info.size.width, info.size.height)
            if (largest > VAULT_THUMB_MAX_EDGE) {
                val scale = largest.toFloat() / VAULT_THUMB_MAX_EDGE
                decoder.setTargetSize(
                    (info.size.width / scale).toInt().coerceAtLeast(1),
                    (info.size.height / scale).toInt().coerceAtLeast(1),
                )
            }
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    } ?: return null
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    val largest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    while (largest / sample > VAULT_THUMB_MAX_EDGE) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.RGB_565
    }
    val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    } ?: return null
    return scaleDownBitmap(decoded, VAULT_THUMB_MAX_EDGE)
}

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
