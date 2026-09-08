package com.example.galeria.features.vault.data

import com.example.galeria.core.encryption.AesGcmEncryptor
import com.example.galeria.core.encryption.Pbkdf2KeyDeriver
import com.example.galeria.core.securestorage.IosSecureKeyStore
import com.example.galeria.features.vault.domain.VaultRepository
import platform.Foundation.NSDate

actual fun createVaultRepository(): VaultRepository {
    val encryptor = AesGcmEncryptor()
    val fileStore = VaultFileStore()
    return VaultRepositoryImpl(
        keyDeriver = Pbkdf2KeyDeriver(),
        encryptor = encryptor,
        secureKeyStore = IosSecureKeyStore(),
        fileStore = fileStore,
        metadataStore = VaultMetadataStore(fileStore, encryptor),
        publicMediaRemover = PublicMediaRemover(),
        publicMediaInserter = PublicMediaInserter(),
    )
}

internal actual fun currentEpochMs(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()
