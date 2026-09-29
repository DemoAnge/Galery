package com.example.galeria.core

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.min

/**
 * Quita el medio del MediaStore y del disco. En Android 11+ el sistema
 * pide confirmación; hay que usar [MediaStore.createDeleteRequest], no
 * [ContentResolver.delete] a secas.
 */
internal object AndroidPublicMediaWipe {

    fun wipe(publicUri: String) {
        wipeAll(listOf(publicUri))
    }

    fun wipeAll(publicUris: List<String>) {
        if (publicUris.isEmpty()) return
        val context = AndroidAppContext.require()
        val resolver = context.contentResolver
        val uris = publicUris.map { it.toUri() }.distinct()
        val leftovers = uris.map { uri -> uri to absolutePath(resolver, uri) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteWithSystemPromptR(resolver, uris)
        } else {
            uris.forEach { deleteLegacy(resolver, it) }
        }

        leftovers.forEach { (_, path) ->
            shred(path)
            if (!path.isNullOrBlank()) {
                MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun deleteWithSystemPromptR(resolver: ContentResolver, uris: List<Uri>) {
        val existing = uris.filter { exists(resolver, it) }
        if (existing.isEmpty()) return
        existing.groupBy(::deleteGroupKey).values.forEach { group ->
            group.chunked(40).forEach { chunk ->
                try {
                    val request = MediaStore.createDeleteRequest(resolver, chunk)
                    val granted = AndroidMediaConsent.request(request.intentSender)
                    check(granted) { DELETE_DENIED }
                } catch (error: Exception) {
                    if (error.message == DELETE_DENIED) throw error
                    chunk.forEach { uri -> fallbackDelete(resolver, uri) }
                }
            }
        }
    }

    private fun deleteLegacy(resolver: ContentResolver, uri: Uri) {
        try {
            val rows = resolver.delete(uri, null, null)
            if (rows > 0) return
        } catch (error: SecurityException) {
            recoverAndDelete(resolver, uri, error)
            return
        }
        error(DELETE_FAILED)
    }

    private fun fallbackDelete(resolver: ContentResolver, uri: Uri) {
        if (!exists(resolver, uri)) return
        try {
            if (resolver.delete(uri, null, null) > 0) return
        } catch (error: SecurityException) {
            recoverAndDelete(resolver, uri, error)
            return
        }
        error(DELETE_FAILED)
    }

    private fun recoverAndDelete(
        resolver: ContentResolver,
        uri: Uri,
        error: SecurityException,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && error is RecoverableSecurityException) {
            recoverFromUserAction(resolver, uri, error)
            return
        }
        throw error
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun recoverFromUserAction(
        resolver: ContentResolver,
        uri: Uri,
        error: RecoverableSecurityException,
    ) {
        val sender = error.userAction.actionIntent.intentSender
        val granted = AndroidMediaConsent.request(sender)
        check(granted) { DELETE_DENIED }
        val rows = resolver.delete(uri, null, null)
        check(rows > 0) { DELETE_FAILED }
    }

    private fun deleteGroupKey(uri: Uri): String {
        val volume = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            volumeNameQ(uri)
        } else {
            null
        } ?: uri.pathSegments.firstOrNull().orEmpty()
        val kind = if (uri.toString().contains("/video", ignoreCase = true)) "v" else "i"
        return "$volume:$kind"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun volumeNameQ(uri: Uri): String? =
        runCatching { MediaStore.getVolumeName(uri) }.getOrNull()

    private fun exists(resolver: ContentResolver, uri: Uri): Boolean {
        return try {
            resolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)
                ?.use { it.moveToFirst() } == true
        } catch (_: Exception) {
            true
        }
    }

    @Suppress("DEPRECATION")
    private fun absolutePath(resolver: ContentResolver, uri: Uri): String? {
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pathProjectionQ()
        } else {
            arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
            )
        }
        return try {
            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                if (dataIndex >= 0 && !cursor.isNull(dataIndex)) {
                    val data = cursor.getString(dataIndex)
                    if (!data.isNullOrBlank()) return@use data
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    pathFromRelativeQ(cursor)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun pathProjectionQ(): Array<String> = arrayOf(
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.DISPLAY_NAME,
        MediaStore.MediaColumns.RELATIVE_PATH,
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun pathFromRelativeQ(cursor: Cursor): String? {
        val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val relativeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
        val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
        val relative = if (relativeIndex >= 0) cursor.getString(relativeIndex) else null
        if (name.isNullOrBlank() || relative.isNullOrBlank()) return null
        return "/storage/emulated/0/${relative.trimEnd('/')}/$name"
    }

    private fun shred(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (!file.isFile || !file.exists()) return
        val dataDir = AndroidAppContext.require().applicationInfo.dataDir
        if (path.startsWith(dataDir)) return
        try {
            RandomAccessFile(file, "rw").use { raf ->
                val zeros = ByteArray(8192)
                var left = raf.length()
                raf.seek(0)
                while (left > 0) {
                    val n = min(zeros.size.toLong(), left).toInt()
                    raf.write(zeros, 0, n)
                    left -= n
                }
                raf.fd.sync()
            }
            file.delete()
        } catch (_: Exception) {
            runCatching { file.delete() }
        }
    }

    private const val DELETE_DENIED = "Public media delete denied"
    private const val DELETE_FAILED = "Public media delete failed"
}
