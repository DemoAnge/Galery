package com.example.galeria.core.encryption

/**
 * Deriva una [DerivedKey] desde PIN + salt (PBKDF2 o Argon2 en la implementación).
 * El PIN va en [CharArray] para poder borrarlo de memoria tras usarlo.
 */
interface KeyDeriver {

    fun derive(pin: CharArray, salt: ByteArray): DerivedKey

    fun generateSalt(): ByteArray

    companion object {
        const val SALT_SIZE_BYTES: Int = 16
        const val PBKDF2_ITERATIONS: Int = 210_000
    }
}
