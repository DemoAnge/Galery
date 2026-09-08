package com.example.galeria.features.vault.domain

class UnlockVaultUseCase(
    private val vaultRepository: VaultRepository,
) {

    fun unlockWithPin(pin: CharArray): Boolean {
        check(vaultRepository.isLockConfigured()) { "Vault lock is not configured" }
        if (vaultRepository.configuredLockMethod() != LockMethod.PIN) return false
        return vaultRepository.verifyAndUnlock(pin)
    }

    fun unlockWithPattern(cells: IntArray): Boolean {
        check(vaultRepository.isLockConfigured()) { "Vault lock is not configured" }
        if (vaultRepository.configuredLockMethod() != LockMethod.PATTERN) return false
        val secret = patternCellsToSecret(cells)
        try {
            return vaultRepository.verifyAndUnlock(secret)
        } finally {
            secret.fill('\u0000')
        }
    }
}
