package com.example.galeria.features.gallery.data

import com.example.galeria.core.AndroidAppContext
import com.example.galeria.features.gallery.domain.UserCollection
import java.io.File

actual class CollectionStore actual constructor() {
    actual fun load(): List<UserCollection> {
        val file = storeFile() ?: return emptyList()
        if (!file.isFile) return emptyList()
        return runCatching { UserCollectionCodec.decode(file.readText()) }.getOrDefault(emptyList())
    }

    actual fun save(collections: List<UserCollection>) {
        val file = storeFile() ?: return
        file.parentFile?.mkdirs()
        file.writeText(UserCollectionCodec.encode(collections))
    }

    private fun storeFile(): File? {
        val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return null
        return File(context.noBackupFilesDir, "user_collections.txt")
    }
}
