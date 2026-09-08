package com.example.galeria.features.vault.domain

/**
 * Si el Vault no tiene fotos ni videos, al salir se borra el candado
 * para que la próxima entrada vuelva a pedir crear el PIN.
 */
fun shouldResetLockWhenLeaving(itemCount: Int): Boolean = itemCount <= 0
