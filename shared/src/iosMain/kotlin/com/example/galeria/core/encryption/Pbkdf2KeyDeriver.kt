package com.example.galeria.core.encryption

import com.example.galeria.cinterop.commoncrypto.CCKeyDerivationPBKDF
import com.example.galeria.cinterop.commoncrypto.CCRandomGenerateBytes
import com.example.galeria.cinterop.commoncrypto.kCCPBKDF2
import com.example.galeria.cinterop.commoncrypto.kCCPRFHmacAlgSHA256
import com.example.galeria.cinterop.commoncrypto.kCCSuccess
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.reinterpret
import platform.posix.uint8_tVar

@OptIn(ExperimentalForeignApi::class)
actual class Pbkdf2KeyDeriver actual constructor() : KeyDeriver {

    override fun derive(pin: CharArray, salt: ByteArray): DerivedKey {
        require(pin.isNotEmpty()) { "PIN must not be empty" }
        require(salt.size == KeyDeriver.SALT_SIZE_BYTES) {
            "salt must be ${KeyDeriver.SALT_SIZE_BYTES} bytes"
        }
        val password = pin.concatToString().encodeToByteArray()
        val derived = ByteArray(DerivedKey.SIZE_BYTES)
        try {
            val status = password.usePinned { passwordPinned ->
                salt.usePinned { saltPinned ->
                    derived.usePinned { derivedPinned ->
                        CCKeyDerivationPBKDF(
                            kCCPBKDF2,
                            passwordPinned.addressOf(0),
                            password.size.convert(),
                            saltPinned.addressOf(0).reinterpret<uint8_tVar>(),
                            salt.size.convert(),
                            kCCPRFHmacAlgSHA256,
                            KeyDeriver.PBKDF2_ITERATIONS.convert(),
                            derivedPinned.addressOf(0).reinterpret<uint8_tVar>(),
                            derived.size.convert(),
                        )
                    }
                }
            }
            check(status == 0) { "PBKDF2 failed: $status" }
            val key = DerivedKey.of(derived)
            derived.fill(0)
            return key
        } finally {
            password.fill(0)
            derived.fill(0)
        }
    }

    override fun generateSalt(): ByteArray {
        val salt = ByteArray(KeyDeriver.SALT_SIZE_BYTES)
        salt.usePinned { pinned ->
            val status = CCRandomGenerateBytes(pinned.addressOf(0), salt.size.convert())
            check(status == kCCSuccess) { "CCRandomGenerateBytes failed: $status" }
        }
        return salt
    }
}
