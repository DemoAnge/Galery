package com.example.galeria.features.vault.data

import com.example.galeria.core.encryption.AesGcmEncryptor
import com.example.galeria.core.encryption.DerivedKey
import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.presentation.deleteTempPlayableVideo
import com.example.galeria.features.vault.presentation.writeTempPlayableVideo

actual fun encryptPublicMediaToVault(
    itemId: String,
    publicUri: String,
    key: DerivedKey,
) {
    val bytes = PublicMediaRemover().readBytes(publicUri)
    try {
        VaultFileStore().write(itemId, AesGcmEncryptor().encrypt(bytes, key))
    } finally {
        bytes.fill(0)
    }
}

actual fun decryptVaultToPlayablePath(
    itemId: String,
    displayName: String,
    key: DerivedKey,
): String {
    val bytes = decryptVaultToPlaintext(itemId, key)
    return try {
        writeTempPlayableVideo(itemId, displayName, bytes)
    } finally {
        bytes.fill(0)
    }
}

actual fun decryptVaultToPlaintext(
    itemId: String,
    key: DerivedKey,
): ByteArray {
    return AesGcmEncryptor().decrypt(VaultFileStore().read(itemId), key)
}

actual fun exportDecryptedVaultItem(
    item: VaultItem,
    key: DerivedKey,
) {
    val path = decryptVaultToPlayablePath(item.id.value, item.displayName, key)
    try {
        PublicMediaInserter().insertFromPath(item, path)
    } finally {
        deleteTempPlayableVideo(path)
    }
}
