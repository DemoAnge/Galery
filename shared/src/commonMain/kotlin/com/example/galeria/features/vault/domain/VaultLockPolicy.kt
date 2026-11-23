package com.example.galeria.features.vault.domain

/**
 * Si el Vault no tiene fotos ni videos, al salir se borra el candado
 * para que la próxima entrada vuelva a pedir crear el PIN.
 */
fun shouldResetLockWhenLeaving(itemCount: Int): Boolean = itemCount <= 0

/**
 * No hay frase, reset ni copia. Si se olvida el PIN o se borra la app,
 * el contenido se pierde con el sandbox (Keystore/Keychain ThisDeviceOnly).
 */
fun allowsPinRecovery(): Boolean = false

/**
 * Tras desinstalar, el sandbox desaparece. Si el marcador no está,
 * cualquier secreto huérfano del Keychain/Keystore se considera basura
 * de una instalación anterior y hay que borrarlo.
 */
fun shouldPurgeOrphanSecrets(hasInstallMarker: Boolean): Boolean = !hasInstallMarker

const val LOCKOUT_START_AFTER_FAILURES: Int = 5

fun lockoutDelayMs(failedAttempts: Int): Long = when {
    failedAttempts < LOCKOUT_START_AFTER_FAILURES -> 0L
    failedAttempts == 5 -> 30_000L
    failedAttempts == 6 -> 60_000L
    failedAttempts == 7 -> 120_000L
    failedAttempts == 8 -> 300_000L
    failedAttempts == 9 -> 900_000L
    else -> 1_800_000L
}

fun lockoutRemainingMs(
    failedAttempts: Int,
    lastFailureEpochMs: Long,
    nowEpochMs: Long,
): Long {
    val delay = lockoutDelayMs(failedAttempts)
    if (delay <= 0L || lastFailureEpochMs <= 0L) return 0L
    return (lastFailureEpochMs + delay - nowEpochMs).coerceAtLeast(0L)
}
