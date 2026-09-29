package com.example.galeria.features.vault.data

import com.example.galeria.core.encryption.DerivedKey
import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultMediaKind

internal const val VAULT_STREAM_MIN_BYTES = 4L * 1024 * 1024

internal fun shouldStreamVaultMedia(kind: VaultMediaKind, sizeBytes: Long): Boolean =
    kind == VaultMediaKind.VIDEO || sizeBytes >= VAULT_STREAM_MIN_BYTES

internal expect fun encryptPublicMediaToVault(
    itemId: String,
    publicUri: String,
    key: DerivedKey,
)

internal expect fun decryptVaultToPlayablePath(
    itemId: String,
    displayName: String,
    key: DerivedKey,
): String

internal expect fun decryptVaultToPlaintext(
    itemId: String,
    key: DerivedKey,
): ByteArray

internal expect fun exportDecryptedVaultItem(
    item: VaultItem,
    key: DerivedKey,
)
