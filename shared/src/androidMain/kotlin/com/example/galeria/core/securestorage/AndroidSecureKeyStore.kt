package com.example.galeria.core.securestorage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.galeria.core.encryption.AesGcmPayload
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secretos envueltos con una clave AES no extraíble en Android Keystore
 * y guardados en [Context.getNoBackupFilesDir].
 */
class AndroidSecureKeyStore(context: Context) : SecureKeyStore {

    private val storageDir = File(context.applicationContext.noBackupFilesDir, DIR_NAME)
    private val lock = Any()

    override fun write(id: String, secret: ByteArray) {
        requireValidSecretId(id)
        val wrapped = wrap(secret)
        synchronized(lock) {
            storageDir.mkdirs()
            val target = fileFor(id)
            val tmp = File(storageDir, "$id.tmp")
            tmp.writeBytes(wrapped)
            if (!tmp.renameTo(target)) {
                target.writeBytes(wrapped)
                tmp.delete()
            }
        }
    }

    override fun read(id: String): ByteArray? {
        requireValidSecretId(id)
        val bytes = synchronized(lock) {
            val file = fileFor(id)
            if (!file.exists()) return null
            file.readBytes()
        }
        return unwrap(bytes)
    }

    override fun delete(id: String) {
        requireValidSecretId(id)
        synchronized(lock) {
            fileFor(id).delete()
        }
    }

    override fun contains(id: String): Boolean {
        requireValidSecretId(id)
        return synchronized(lock) { fileFor(id).exists() }
    }

    private fun fileFor(id: String): File = File(storageDir, id)

    private fun wrap(secret: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val nonce = cipher.iv
        val ciphertext = cipher.doFinal(secret)
        return nonce + ciphertext
    }

    private fun unwrap(blob: ByteArray): ByteArray {
        val minSize = AesGcmPayload.NONCE_SIZE_BYTES + AesGcmPayload.TAG_SIZE_BYTES
        require(blob.size >= minSize) { "Wrapped secret is truncated" }
        val nonce = blob.copyOfRange(0, AesGcmPayload.NONCE_SIZE_BYTES)
        val ciphertext = blob.copyOfRange(AesGcmPayload.NONCE_SIZE_BYTES, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            wrappingKey(),
            GCMParameterSpec(AesGcmPayload.TAG_SIZE_BYTES * 8, nonce),
        )
        return cipher.doFinal(ciphertext)
    }

    private fun wrappingKey(): SecretKey {
        synchronized(lock) {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(WRAPPING_KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing
            return generateWrappingKey()
        }
    }

    private fun generateWrappingKey(): SecretKey {
        var lastError: Exception? = null
        for (keySize in WRAP_KEY_SIZES) {
            try {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    WRAPPING_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setKeySize(keySize)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(spec)
                return keyGenerator.generateKey()
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Unable to generate wrapping key")
    }
}

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val WRAPPING_KEY_ALIAS = "com.example.galeria.wrapping"
private const val DIR_NAME = "securestore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private val WRAP_KEY_SIZES = intArrayOf(256, 128)
