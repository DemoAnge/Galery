package com.example.galeria.core.encryption

import java.io.InputStream
import java.io.OutputStream
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

    fun encryptStream(input: InputStream, output: OutputStream, key: DerivedKey) {
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
            output.write(nonce)
            val buf = ByteArray(CHUNK)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                if (n == 0) continue
                val chunk = cipher.update(buf, 0, n)
                if (chunk != null && chunk.isNotEmpty()) output.write(chunk)
            }
            val last = cipher.doFinal()
            if (last.isNotEmpty()) output.write(last)
            output.flush()
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decryptStream(input: InputStream, output: OutputStream, key: DerivedKey) {
        val keyBytes = key.asBytes()
        try {
            val nonce = ByteArray(AesGcmPayload.NONCE_SIZE_BYTES)
            input.readExact(nonce)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(AesGcmPayload.TAG_SIZE_BYTES * 8, nonce),
            )
            val buf = ByteArray(CHUNK)
            var leftover = ByteArray(0)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                if (n == 0) continue
                val merged = ByteArray(leftover.size + n)
                leftover.copyInto(merged)
                buf.copyInto(merged, leftover.size, 0, n)
                leftover.fill(0)
                if (merged.size <= AesGcmPayload.TAG_SIZE_BYTES) {
                    leftover = merged
                    continue
                }
                val emitLen = merged.size - AesGcmPayload.TAG_SIZE_BYTES
                val chunk = cipher.update(merged, 0, emitLen)
                if (chunk != null && chunk.isNotEmpty()) output.write(chunk)
                leftover = merged.copyOfRange(emitLen, merged.size)
                merged.fill(0)
            }
            check(leftover.size == AesGcmPayload.TAG_SIZE_BYTES) { "Encrypted file is truncated" }
            val last = cipher.doFinal(leftover, 0, leftover.size)
            leftover.fill(0)
            if (last.isNotEmpty()) output.write(last)
            output.flush()
        } finally {
            keyBytes.fill(0)
        }
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val CHUNK = 64 * 1024
    }
}

private fun InputStream.readExact(buffer: ByteArray) {
    var offset = 0
    while (offset < buffer.size) {
        val n = read(buffer, offset, buffer.size - offset)
        if (n < 0) error("Encrypted file is truncated")
        offset += n
    }
}
