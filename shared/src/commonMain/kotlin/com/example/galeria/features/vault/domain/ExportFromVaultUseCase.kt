package com.example.galeria.features.vault.domain

class ExportFromVaultUseCase(
    private val vaultRepository: VaultRepository,
) {

    operator fun invoke(id: VaultItemId) {
        invoke(listOf(id))
    }

    operator fun invoke(ids: List<VaultItemId>) {
        if (ids.isEmpty()) return
        check(vaultRepository.isLockConfigured()) { "Vault lock is not configured" }
        check(vaultRepository.isUnlocked()) { "Vault is locked" }
        ids.forEach { vaultRepository.exportToPublicStorage(it) }
    }
}
