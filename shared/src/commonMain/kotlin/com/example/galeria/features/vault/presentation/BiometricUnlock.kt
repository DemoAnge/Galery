package com.example.galeria.features.vault.presentation

/**
 * Atajo biométrico. Nunca es el único método: exige PIN/patrón ya configurado
 * y una DEK envuelta con una clave ligada a biometría.
 */
expect class BiometricUnlock() {
    fun isAvailable(): Boolean
    fun wrap(dek: ByteArray, onResult: (ByteArray?) -> Unit)
    fun unwrap(wrapped: ByteArray, onResult: (ByteArray?) -> Unit)
    fun cancel()
}
