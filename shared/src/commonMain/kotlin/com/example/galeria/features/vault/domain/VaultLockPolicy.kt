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
