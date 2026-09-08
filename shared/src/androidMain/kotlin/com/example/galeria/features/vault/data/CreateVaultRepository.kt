package com.example.galeria.features.vault.data

import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.encryption.AesGcmEncryptor
import com.example.galeria.core.encryption.Pbkdf2KeyDeriver
import com.example.galeria.core.securestorage.AndroidSecureKeyStore
import com.example.galeria.features.vault.domain.VaultRepository

actual fun createVaultRepository(): VaultRepository {
    val context = AndroidAppContext.require()
    val encryptor = AesGcmEncryptor()
    val fileStore = VaultFileStore()
    return VaultRepositoryImpl(
        keyDeriver = Pbkdf2KeyDeriver(),
        encryptor = encryptor,
        secureKeyStore = AndroidSecureKeyStore(context),
        fileStore = fileStore,
        metadataStore = VaultMetadataStore(fileStore, encryptor),
        publicMediaRemover = PublicMediaRemover(),
        publicMediaInserter = PublicMediaInserter(),
    )
}

internal actual fun currentEpochMs(): Long = System.currentTimeMillis()
