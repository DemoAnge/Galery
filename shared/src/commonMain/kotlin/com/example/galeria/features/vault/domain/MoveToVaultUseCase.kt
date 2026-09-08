package com.example.galeria.features.vault.domain

class MoveToVaultUseCase(
    private val vaultRepository: VaultRepository,
) {

    operator fun invoke(source: ExternalMediaSource): VaultItem =
        invoke(listOf(source)).first()

    operator fun invoke(sources: List<ExternalMediaSource>): List<VaultItem> {
        check(vaultRepository.isLockConfigured()) { "Vault lock is not configured" }
        check(vaultRepository.canHideWithoutUnlock()) { "Vault is locked" }
        return vaultRepository.moveAllToVaultRemovingFromPublicStorage(sources)
    }
}
