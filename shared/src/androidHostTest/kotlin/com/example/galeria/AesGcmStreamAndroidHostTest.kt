package com.example.galeria

import com.example.galeria.core.encryption.AesGcmEncryptor
import com.example.galeria.core.encryption.DerivedKey
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFails

class AesGcmStreamAndroidHostTest {

    private val encryptor = AesGcmEncryptor()
    private val key = DerivedKey.of(ByteArray(32) { it.toByte() })

    @Test
    fun streamRoundtripMatchesOneshotForSmallPayload() {
        val plaintext = "galeria-vault".encodeToByteArray()
        val streamed = streamRoundtrip(plaintext)
        val oneshot = encryptor.decrypt(encryptor.encrypt(plaintext, key), key)
        assertContentEquals(plaintext, streamed)
        assertContentEquals(plaintext, oneshot)
    }

    @Test
    fun streamRoundtripHandlesMultipleChunks() {
        val plaintext = Random.nextBytes(200_000)
        assertContentEquals(plaintext, streamRoundtrip(plaintext))
    }

    @Test
    fun truncatedCiphertextFailsWithoutCrashing() {
        val encrypted = ByteArrayOutputStream()
        encryptor.encryptStream(ByteArrayInputStream(Random.nextBytes(4096)), encrypted, key)
        val cut = encrypted.toByteArray().copyOf(20)
        assertFails {
            encryptor.decryptStream(ByteArrayInputStream(cut), ByteArrayOutputStream(), key)
        }
    }

    private fun streamRoundtrip(plaintext: ByteArray): ByteArray {
        val encrypted = ByteArrayOutputStream()
        encryptor.encryptStream(ByteArrayInputStream(plaintext), encrypted, key)
        val decrypted = ByteArrayOutputStream()
        encryptor.decryptStream(ByteArrayInputStream(encrypted.toByteArray()), decrypted, key)
        return decrypted.toByteArray()
    }
}
