package com.example.galeria.features.gallery.presentation

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.net.toUri
import androidx.compose.ui.graphics.asImageBitmap
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.withMediaMetadataRetriever
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val THUMB_CACHE_MAX = 36
private val thumbLock = Any()
private val thumbCache = object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
        size > THUMB_CACHE_MAX
}

actual fun decodePublicImage(publicUri: String, maxEdge: Int): ImageBitmap? {
    val key = "$publicUri#$maxEdge"
    synchronized(thumbLock) { thumbCache[key] }?.let { return it }
    val decoded = decodeBitmap(publicUri, maxEdge)?.asImageBitmap() ?: return null
    synchronized(thumbLock) { thumbCache[key] = decoded }
    return decoded
}

actual fun loadEditJpeg(publicUri: String, maxEdge: Int): ByteArray? {
    val bitmap = decodeBitmap(publicUri, maxEdge) ?: return null
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    return out.toByteArray()
}

private fun decodeBitmap(publicUri: String, maxEdge: Int): Bitmap? {
    val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return null
    val resolver = context.contentResolver
    val uri = publicUri.toUri()
    if (isVideoUri(resolver, uri)) {
        return decodeVideoFrame(context, resolver, uri, maxEdge)
    }
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largest = max(info.size.width, info.size.height)
                if (maxEdge > 0 && largest > maxEdge) {
                    val scale = largest.toFloat() / maxEdge
                    decoder.setTargetSize(
                        (info.size.width / scale).toInt().coerceAtLeast(1),
                        (info.size.height / scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            decodeWithFactory(resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null, maxEdge)
        }
    } catch (_: Exception) {
        try {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            decodeWithFactory(bytes, maxEdge)
        } catch (_: Exception) {
            null
        }
    }
}

private fun isVideoUri(resolver: ContentResolver, uri: Uri): Boolean {
    val mime = resolver.getType(uri).orEmpty()
    if (mime.startsWith("video/")) return true
    val path = uri.path.orEmpty().lowercase()
    return path.contains("/video") || path.endsWith(".mp4") || path.endsWith(".mov") ||
        path.endsWith(".webm") || path.endsWith(".mkv") || path.endsWith(".m4v")
}

private fun decodeVideoFrame(
    context: Context,
    resolver: ContentResolver,
    uri: Uri,
    maxEdge: Int,
): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val size = maxEdge.coerceIn(64, 512)
        runCatching { resolver.loadThumbnail(uri, Size(size, size), null) }.getOrNull()?.let { return it }
    }
    return withMediaMetadataRetriever { retriever ->
        retriever.setDataSource(context, uri)
        val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime
        frame?.let { scaleDown(it, maxEdge) }
    }
}

private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
    if (maxEdge <= 0) return bitmap
    val largest = max(bitmap.width, bitmap.height).coerceAtLeast(1)
    if (largest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / largest
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true,
    )
}

private fun decodeWithFactory(bytes: ByteArray, maxEdge: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    val largest = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
    if (maxEdge > 0) {
        while (largest / sample > maxEdge) sample *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = if (maxEdge <= 512) Bitmap.Config.RGB_565 else Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
