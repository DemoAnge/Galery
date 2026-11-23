package com.example.galeria.features.vault.domain

class ChangeLockUseCase(
    private val vaultRepository: VaultRepository,
) {

    fun changeToPin(currentSecret: CharArray, pin: CharArray) {
        requirePin(pin)
        check(vaultRepository.isLockConfigured()) { "Vault lock is not configured" }
        check(vaultRepository.isUnlocked()) { "Vault is locked" }
        vaultRepository.changeLock(currentSecret, LockMethod.PIN, pin)
    }

    fun changeToPattern(currentSecret: CharArray, cells: IntArray) {
        requirePattern(cells)
        check(vaultRepository.isLockConfigured()) { "Vault lock is not configured" }
        check(vaultRepository.isUnlocked()) { "Vault is locked" }
        val secret = patternCellsToSecret(cells)
        try {
            vaultRepository.changeLock(currentSecret, LockMethod.PATTERN, secret)
        } finally {
            secret.fill('\u0000')
        }
    }
}
