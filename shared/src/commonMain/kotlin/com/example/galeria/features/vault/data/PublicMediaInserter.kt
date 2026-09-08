package com.example.galeria.features.vault.data

import com.example.galeria.features.vault.domain.VaultItem

/**
 * Reinserta bytes descifrados en MediaStore / PHPhotoLibrary.
 */
expect class PublicMediaInserter() {
    fun insert(item: VaultItem, plaintext: ByteArray)
}
