package com.example.galeria.features.vault.domain

class ReplaceVaultImageUseCase(
    private val vaultRepository: VaultRepository,
) {

    operator fun invoke(id: VaultItemId, jpeg: ByteArray) {
        check(vaultRepository.isLockConfigured()) { "Vault lock is not configured" }
        check(vaultRepository.isUnlocked()) { "Vault is locked" }
        vaultRepository.replaceEncryptedImage(id, jpeg)
    }
}
