package com.example.galeria.features.vault.data

import androidx.core.net.toUri
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.AndroidPublicMediaWipe

actual class PublicMediaRemover actual constructor() {

    actual fun readBytes(publicUri: String): ByteArray {
        val resolver = AndroidAppContext.require().contentResolver
        val uri = publicUri.toUri()
        val stream = resolver.openInputStream(uri) ?: error("Public media read failed")
        return stream.use { it.readBytes() }
    }

    actual fun delete(publicUri: String) {
        AndroidPublicMediaWipe.wipe(publicUri)
    }

    actual fun deleteAll(publicUris: List<String>) {
        AndroidPublicMediaWipe.wipeAll(publicUris)
    }
}
