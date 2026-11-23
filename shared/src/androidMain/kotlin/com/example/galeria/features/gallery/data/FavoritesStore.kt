package com.example.galeria.features.gallery.data

import com.example.galeria.core.AndroidAppContext
import java.io.File

actual class FavoritesStore actual constructor() {
    actual fun load(): Set<String> {
        val file = storeFile() ?: return emptySet()
        if (!file.isFile) return emptySet()
        return runCatching { FavoritesCodec.decode(file.readText()) }.getOrDefault(emptySet())
    }

    actual fun save(ids: Set<String>) {
        val file = storeFile() ?: return
        file.parentFile?.mkdirs()
        file.writeText(FavoritesCodec.encode(ids))
    }

    private fun storeFile(): File? {
        val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return null
        return File(context.noBackupFilesDir, "favorites.txt")
    }
}
