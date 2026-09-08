package com.example.galeria.features.vault.domain

class ExportFromVaultUseCase(
    private val vaultRepository: VaultRepository,
) {

    operator fun invoke(id: VaultItemId) {
        check(vaultRepository.isLockConfigured()) { "Vault lock is not configured" }
        check(vaultRepository.isUnlocked()) { "Vault is locked" }
        vaultRepository.exportToPublicStorage(id)
    }
}
