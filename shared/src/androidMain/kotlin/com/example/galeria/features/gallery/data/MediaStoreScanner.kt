package com.example.galeria.features.gallery.data

import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.AndroidPublicMediaWipe
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.features.gallery.domain.MediaRepository
import com.example.galeria.features.gallery.domain.displaySize
import com.example.galeria.features.gallery.domain.preferEmbeddedVideoTitle
import com.example.galeria.features.gallery.domain.videoNeedsFileMetadata

class MediaStoreScanner : MediaRepository {

    private var embeddedReadsThisScan = 0

    override fun scan(): List<MediaItem> {
        embeddedReadsThisScan = 0
        val context = AndroidAppContext.require()
        val resolver = context.contentResolver
        val volumes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getExternalVolumeNames(context)
        } else {
            setOf("external")
        }
        val items = ArrayList<MediaItem>()
        volumes.forEach { volume ->
            items += scanCollection(
                resolver = resolver,
                collection = imageCollection(volume),
                kind = GalleryMediaKind.IMAGE,
            )
            items += scanCollection(
                resolver = resolver,
                collection = videoCollection(volume),
                kind = GalleryMediaKind.VIDEO,
            )
        }
        if (items.isEmpty()) {
            items += scanCollection(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, GalleryMediaKind.IMAGE)
            items += scanCollection(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, GalleryMediaKind.VIDEO)
        }
        return items.distinctBy { it.id }
    }

    private fun scanCollection(
        resolver: android.content.ContentResolver,
        collection: Uri,
        kind: GalleryMediaKind,
    ): List<MediaItem> {
        val rich = queryItems(resolver, collection, kind, richProjection(kind))
        if (rich != null) return rich
        return queryItems(resolver, collection, kind, minimalProjection()) ?: emptyList()
    }

    private fun queryItems(
        resolver: android.content.ContentResolver,
        collection: Uri,
        kind: GalleryMediaKind,
        projection: Array<String>,
    ): List<MediaItem>? {
        val selection: String?
        val selectionArgs: Array<String>?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && projection.contains(MediaStore.MediaColumns.IS_PENDING)) {
            selection = "${MediaStore.MediaColumns.IS_PENDING}=?"
            selectionArgs = arrayOf("0")
        } else {
            selection = null
            selectionArgs = null
        }
        val sort = when {
            projection.contains(MediaStore.MediaColumns.DATE_TAKEN) ->
                "${MediaStore.MediaColumns.DATE_TAKEN} DESC"
            else -> "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        }
        return try {
            val cursor = resolver.query(collection, projection, selection, selectionArgs, sort) ?: return emptyList()
            val items = ArrayList<MediaItem>()
            cursor.use {
                while (it.moveToNext()) {
                    runCatching { items += it.toMediaItem(collection, kind) }
                }
            }
            items
        } catch (_: Exception) {
            null
        }
    }

    private fun richProjection(kind: GalleryMediaKind): Array<String> = buildList {
        add(MediaStore.MediaColumns._ID)
        add(MediaStore.MediaColumns.DISPLAY_NAME)
        add(MediaStore.MediaColumns.SIZE)
        add(MediaStore.MediaColumns.DATE_ADDED)
        add(MediaStore.MediaColumns.DATE_MODIFIED)
        add(MediaStore.MediaColumns.DATE_TAKEN)
        add(MediaStore.MediaColumns.MIME_TYPE)
        add(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        add(MediaStore.MediaColumns.WIDTH)
        add(MediaStore.MediaColumns.HEIGHT)
        add(MediaStore.MediaColumns.ORIENTATION)
        if (kind == GalleryMediaKind.VIDEO) {
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.TITLE)
            add(MediaStore.Video.Media.ARTIST)
            add(MediaStore.Video.Media.ALBUM)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.MediaColumns.RELATIVE_PATH)
            add(MediaStore.MediaColumns.IS_PENDING)
            add(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
        } else {
            @Suppress("DEPRECATION")
            add(MediaStore.MediaColumns.DATA)
        }
    }.toTypedArray()

    private fun minimalProjection(): Array<String> = arrayOf(
        MediaStore.MediaColumns._ID,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.MIME_TYPE,
        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
    )

    private fun imageCollection(volume: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(volume)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun videoCollection(volume: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(volume)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    }

    override fun readBytes(publicUri: String): ByteArray {
        val resolver = AndroidAppContext.require().contentResolver
        val stream = resolver.openInputStream(publicUri.toUri()) ?: error("Public media read failed")
        return stream.use { it.readBytes() }
    }

    override fun delete(publicUri: String) {
        AndroidPublicMediaWipe.wipe(publicUri)
    }

    override fun deleteAll(publicUris: List<String>) {
        AndroidPublicMediaWipe.wipeAll(publicUris)
    }

    override fun saveJpeg(displayName: String, bytes: ByteArray) {
        val resolver = AndroidAppContext.require().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Galeria")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Public media insert failed")
        try {
            val out = resolver.openOutputStream(uri) ?: error("Public media insert failed")
            out.use { it.write(bytes) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    override fun saveVideo(displayName: String, path: String) {
        val file = java.io.File(path)
        check(file.isFile) { "Public media insert failed" }
        val resolver = AndroidAppContext.require().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, videoMime(displayName))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Galeria")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Public media insert failed")
        try {
            val out = resolver.openOutputStream(uri) ?: error("Public media insert failed")
            out.use { output ->
                file.inputStream().buffered().use { input -> input.copyTo(output) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun videoMime(displayName: String): String {
        val name = displayName.lowercase()
        return when {
            name.endsWith(".mov") -> "video/quicktime"
            name.endsWith(".webm") -> "video/webm"
            else -> "video/mp4"
        }
    }

    private fun Cursor.toMediaItem(collection: Uri, kind: GalleryMediaKind): MediaItem {
        val id = getLong(getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
        val uri = ContentUris.withAppendedId(collection, id).toString()
        val fileName = stringOrEmpty(MediaStore.MediaColumns.DISPLAY_NAME).ifEmpty { "media" }
        val taggedTitle = if (kind == GalleryMediaKind.VIDEO) {
            stringOrNull(MediaStore.Video.Media.TITLE)?.takeIf { it.isNotBlank() && it != fileName }
        } else {
            null
        }
        val displayName = taggedTitle ?: fileName
        val size = longOrZero(MediaStore.MediaColumns.SIZE)
        val bucket = stringOrNull(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        val owner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stringOrNull(MediaStore.MediaColumns.OWNER_PACKAGE_NAME)
        } else {
            null
        }
        val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            stringOrNull(MediaStore.MediaColumns.RELATIVE_PATH)
                ?: stringOrNull(MediaStore.MediaColumns.DATA)
        } else {
            @Suppress("DEPRECATION")
            stringOrNull(MediaStore.MediaColumns.DATA)
        }
        val (width, height) = displaySize(
            widthPx = longOrZero(MediaStore.MediaColumns.WIDTH).toInt(),
            heightPx = longOrZero(MediaStore.MediaColumns.HEIGHT).toInt(),
            orientationDegrees = longOrZero(MediaStore.MediaColumns.ORIENTATION).toInt(),
        )
        val takenRaw = longOrZero(MediaStore.MediaColumns.DATE_TAKEN)
        val hasTakenDate = takenRaw > 0L
        val duration = longOrZero(MediaStore.Video.Media.DURATION)
            .takeIf { it > 0L }
            ?: longOrZero(MediaStore.MediaColumns.DURATION)
        val item = MediaItem(
            id = uri,
            publicUri = uri,
            displayName = displayName,
            mediaKind = kind,
            capturedAtEpochMs = capturedAtEpochMs(hasTakenDate, takenRaw),
            sizeBytes = size,
            relativePath = relativePath,
            bucketName = bucket,
            ownerPackage = owner,
            widthPx = width,
            heightPx = height,
            durationMs = duration,
        )
        return if (kind == GalleryMediaKind.VIDEO) {
            enrichDownloadedVideo(item, hasTakenDate)
        } else {
            item
        }
    }

    private fun enrichDownloadedVideo(item: MediaItem, hasTakenDate: Boolean): MediaItem {
        if (!videoNeedsFileMetadata(
                durationMs = item.durationMs,
                widthPx = item.widthPx,
                heightPx = item.heightPx,
                hasTakenDate = hasTakenDate,
                relativePath = item.relativePath,
                bucketName = item.bucketName,
            )
        ) {
            return item
        }
        if (embeddedReadsThisScan >= MAX_EMBEDDED_METADATA_PER_SCAN) return item
        embeddedReadsThisScan += 1
        val embedded = readEmbeddedVideoMetadata(item.publicUri) ?: return item
        val (width, height) = when {
            item.widthPx > 0 && item.heightPx > 0 -> item.widthPx to item.heightPx
            else -> embedded.widthPx to embedded.heightPx
        }
        return item.copy(
            displayName = preferEmbeddedVideoTitle(item.displayName, embedded.title),
            capturedAtEpochMs = when {
                hasTakenDate && item.capturedAtEpochMs > 0L -> item.capturedAtEpochMs
                embedded.capturedAtEpochMs > 0L -> embedded.capturedAtEpochMs
                else -> item.capturedAtEpochMs
            },
            widthPx = width,
            heightPx = height,
            durationMs = if (item.durationMs > 0L) item.durationMs else embedded.durationMs,
        )
    }

    private fun Cursor.capturedAtEpochMs(hasTakenDate: Boolean, takenRaw: Long): Long {
        if (hasTakenDate) return normalizeEpochMs(takenRaw)
        val modified = longOrZero(MediaStore.MediaColumns.DATE_MODIFIED)
        if (modified > 0L) return normalizeEpochMs(modified)
        return normalizeEpochMs(longOrZero(MediaStore.MediaColumns.DATE_ADDED))
    }

    private fun normalizeEpochMs(value: Long): Long {
        // MediaStore DATE_TAKEN is ms; DATE_ADDED / DATE_MODIFIED are seconds.
        return if (value in 1L until 10_000_000_000L) value * 1000L else value
    }

    private fun Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        if (index < 0 || isNull(index)) return null
        return getString(index)
    }

    private fun Cursor.stringOrEmpty(column: String): String = stringOrNull(column).orEmpty()

    private companion object {
        const val MAX_EMBEDDED_METADATA_PER_SCAN = 20
    }

    private fun Cursor.longOrZero(column: String): Long {
        val index = getColumnIndex(column)
        if (index < 0 || isNull(index)) return 0L
        return getLong(index)
    }
}
