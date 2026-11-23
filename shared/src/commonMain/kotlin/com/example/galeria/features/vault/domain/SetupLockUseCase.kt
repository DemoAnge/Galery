package com.example.galeria.features.vault.domain

class SetupLockUseCase(
    private val vaultRepository: VaultRepository,
) {

    fun setupPin(pin: CharArray) {
        requirePin(pin)
        check(!vaultRepository.isLockConfigured()) { "Vault lock is already configured" }
        vaultRepository.setupLock(LockMethod.PIN, pin)
    }

    fun setupPattern(cells: IntArray) {
        requirePattern(cells)
        check(!vaultRepository.isLockConfigured()) { "Vault lock is already configured" }
        val secret = patternCellsToSecret(cells)
        try {
            vaultRepository.setupLock(LockMethod.PATTERN, secret)
        } finally {
            secret.fill('\u0000')
        }
    }

}

internal fun requirePin(pin: CharArray) {
    require(pin.size == LockMethod.PIN_DIGITS) {
        "PIN must be ${LockMethod.PIN_DIGITS} digits"
    }
    require(pin.all { it in '0'..'9' }) { "PIN must be digits" }
}

internal fun requirePattern(cells: IntArray) {
    require(cells.size >= LockMethod.PATTERN_MIN_CELLS) {
        "Pattern must visit at least ${LockMethod.PATTERN_MIN_CELLS} cells"
    }
    require(cells.all { it in 0 until LockMethod.PATTERN_CELL_COUNT }) {
        "Pattern cell out of range"
    }
    require(cells.toSet().size == cells.size) { "Pattern cells must be unique" }
}
