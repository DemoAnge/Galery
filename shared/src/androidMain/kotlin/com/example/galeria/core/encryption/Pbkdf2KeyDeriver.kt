package com.example.galeria.core.encryption

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

actual class Pbkdf2KeyDeriver actual constructor() : KeyDeriver {

    override fun derive(pin: CharArray, salt: ByteArray): DerivedKey {
        require(pin.isNotEmpty()) { "PIN must not be empty" }
        require(salt.size == KeyDeriver.SALT_SIZE_BYTES) {
            "salt must be ${KeyDeriver.SALT_SIZE_BYTES} bytes"
        }
        val password = pin.concatToString().encodeToByteArray()
        try {
            val raw = pbkdf2HmacSha256(
                password = password,
                salt = salt,
                iterations = KeyDeriver.PBKDF2_ITERATIONS,
                dkLen = DerivedKey.SIZE_BYTES,
            )
            val key = DerivedKey.of(raw)
            raw.fill(0)
            return key
        } finally {
            password.fill(0)
        }
    }

    override fun generateSalt(): ByteArray {
        val salt = ByteArray(KeyDeriver.SALT_SIZE_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun pbkdf2HmacSha256(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        dkLen: Int,
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        val hLen = mac.macLength
        val blockCount = (dkLen + hLen - 1) / hLen
        val derived = ByteArray(dkLen)
        var offset = 0
        for (blockIndex in 1..blockCount) {
            val block = pbkdf2Block(mac, salt, iterations, blockIndex)
            val toCopy = min(hLen, dkLen - offset)
            block.copyInto(derived, offset, 0, toCopy)
            offset += toCopy
        }
        return derived
    }

    private fun pbkdf2Block(
        mac: Mac,
        salt: ByteArray,
        iterations: Int,
        blockIndex: Int,
    ): ByteArray {
        val indexBytes = byteArrayOf(
            (blockIndex ushr 24).toByte(),
            (blockIndex ushr 16).toByte(),
            (blockIndex ushr 8).toByte(),
            blockIndex.toByte(),
        )
        mac.update(salt)
        var u = mac.doFinal(indexBytes)
        val result = u.copyOf()
        repeat(iterations - 1) {
            u = mac.doFinal(u)
            for (i in result.indices) {
                result[i] = (result[i].toInt() xor u[i].toInt()).toByte()
            }
        }
        return result
    }
}
