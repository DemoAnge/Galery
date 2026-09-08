package com.example.galeria.features.vault.data

import com.example.galeria.core.encryption.AesGcmPayload

/**
 * Archivos cifrados en el sandbox de la app (noBackup / excluded from backup).
 */
expect class VaultFileStore() {
    fun write(itemId: String, payload: AesGcmPayload)
    fun read(itemId: String): AesGcmPayload
    fun delete(itemId: String)
    fun exists(itemId: String): Boolean
}

internal const val VAULT_METADATA_FILE_ID: String = "vault.metadata"

internal fun AesGcmPayload.toFileBytes(): ByteArray = nonce + ciphertext

internal fun ByteArray.toAesGcmPayload(): AesGcmPayload {
    val minSize = AesGcmPayload.NONCE_SIZE_BYTES + AesGcmPayload.TAG_SIZE_BYTES
    require(size >= minSize) { "Encrypted file is truncated" }
    return AesGcmPayload(
        nonce = copyOfRange(0, AesGcmPayload.NONCE_SIZE_BYTES),
        ciphertext = copyOfRange(AesGcmPayload.NONCE_SIZE_BYTES, size),
    )
}
