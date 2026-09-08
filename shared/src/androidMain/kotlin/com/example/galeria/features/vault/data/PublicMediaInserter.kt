package com.example.galeria.features.vault.data

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultMediaKind

actual class PublicMediaInserter actual constructor() {

    actual fun insert(item: VaultItem, plaintext: ByteArray) {
        val context = AndroidAppContext.require()
        val resolver = context.contentResolver
        val image = item.mediaKind == VaultMediaKind.IMAGE
        val collection = if (image) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType(item))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val dir = if (image) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/Galeria")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values) ?: error("Public media insert failed")
        try {
            val out = resolver.openOutputStream(uri) ?: error("Public media insert failed")
            out.use { it.write(plaintext) }
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

    private fun mimeType(item: VaultItem): String {
        val name = item.displayName.lowercase()
        return when (item.mediaKind) {
            VaultMediaKind.IMAGE -> when {
                name.endsWith(".png") -> "image/png"
                name.endsWith(".webp") -> "image/webp"
                name.endsWith(".gif") -> "image/gif"
                name.endsWith(".heic") -> "image/heic"
                else -> "image/jpeg"
            }
            VaultMediaKind.VIDEO -> when {
                name.endsWith(".mov") -> "video/quicktime"
                name.endsWith(".webm") -> "video/webm"
                else -> "video/mp4"
            }
        }
    }
}
