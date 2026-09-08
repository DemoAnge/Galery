package com.example.galeria.core.encryption

/**
 * Cifrado autenticado AES-256-GCM. El nonce no se reutiliza entre llamadas.
 */
interface Encryptor {

    fun encrypt(plaintext: ByteArray, key: DerivedKey): AesGcmPayload

    fun decrypt(payload: AesGcmPayload, key: DerivedKey): ByteArray
}

/**
 * Paquete GCM: nonce de 12 bytes + ciphertext con tag de autenticación.
 */
class AesGcmPayload(
    val nonce: ByteArray,
    val ciphertext: ByteArray,
) {
    init {
        require(nonce.size == NONCE_SIZE_BYTES) { "GCM nonce must be $NONCE_SIZE_BYTES bytes" }
        require(ciphertext.isNotEmpty()) { "ciphertext must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AesGcmPayload) return false
        return nonce.contentEquals(other.nonce) && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int = 31 * nonce.contentHashCode() + ciphertext.contentHashCode()

    override fun toString(): String = "AesGcmPayload(nonceSize=${nonce.size}, ciphertextSize=${ciphertext.size})"

    companion object {
        const val NONCE_SIZE_BYTES: Int = 12
        const val TAG_SIZE_BYTES: Int = 16
    }
}
