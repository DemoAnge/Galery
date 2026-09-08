package com.example.galeria.core.encryption

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

actual class AesGcmEncryptor actual constructor() : Encryptor {

    override fun encrypt(plaintext: ByteArray, key: DerivedKey): AesGcmPayload {
        val keyBytes = key.asBytes()
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val nonce = ByteArray(AesGcmPayload.NONCE_SIZE_BYTES)
            SecureRandom().nextBytes(nonce)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(AesGcmPayload.TAG_SIZE_BYTES * 8, nonce),
            )
            return AesGcmPayload(nonce, cipher.doFinal(plaintext))
        } finally {
            keyBytes.fill(0)
        }
    }

    override fun decrypt(payload: AesGcmPayload, key: DerivedKey): ByteArray {
        val keyBytes = key.asBytes()
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(AesGcmPayload.TAG_SIZE_BYTES * 8, payload.nonce),
            )
            return cipher.doFinal(payload.ciphertext)
        } finally {
            keyBytes.fill(0)
        }
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
