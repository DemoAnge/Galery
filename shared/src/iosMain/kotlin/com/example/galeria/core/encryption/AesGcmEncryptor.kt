package com.example.galeria.core.encryption

import com.example.galeria.cinterop.commoncrypto.CCCryptorGCMOneshotDecrypt
import com.example.galeria.cinterop.commoncrypto.CCCryptorGCMOneshotEncrypt
import com.example.galeria.cinterop.commoncrypto.CCRandomGenerateBytes
import com.example.galeria.cinterop.commoncrypto.kCCAlgorithmAES
import com.example.galeria.cinterop.commoncrypto.kCCSuccess
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual class AesGcmEncryptor actual constructor() : Encryptor {

    override fun encrypt(plaintext: ByteArray, key: DerivedKey): AesGcmPayload {
        val keyBytes = key.asBytes()
        val nonce = ByteArray(AesGcmPayload.NONCE_SIZE_BYTES)
        val ciphertext = ByteArray(plaintext.size)
        val tag = ByteArray(AesGcmPayload.TAG_SIZE_BYTES)
        try {
            nonce.usePinned { noncePinned ->
                val randomStatus = CCRandomGenerateBytes(
                    noncePinned.addressOf(0),
                    nonce.size.convert(),
                )
                check(randomStatus == kCCSuccess) { "CCRandomGenerateBytes failed: $randomStatus" }
            }
            val status = keyBytes.usePointer { keyPtr ->
                nonce.usePointer { noncePtr ->
                    plaintext.usePointer { plaintextPtr ->
                        ciphertext.usePointer { ciphertextPtr ->
                            tag.usePinned { tagPinned ->
                                CCCryptorGCMOneshotEncrypt(
                                    kCCAlgorithmAES,
                                    keyPtr,
                                    keyBytes.size.convert(),
                                    noncePtr,
                                    nonce.size.convert(),
                                    null,
                                    0.convert(),
                                    plaintextPtr,
                                    plaintext.size.convert(),
                                    ciphertextPtr,
                                    tagPinned.addressOf(0),
                                    tag.size.convert(),
                                )
                            }
                        }
                    }
                }
            }
            check(status == kCCSuccess) { "AES-GCM encrypt failed: $status" }
            return AesGcmPayload(nonce, ciphertext + tag)
        } finally {
            keyBytes.fill(0)
        }
    }

    override fun decrypt(payload: AesGcmPayload, key: DerivedKey): ByteArray {
        require(payload.ciphertext.size >= AesGcmPayload.TAG_SIZE_BYTES) { "ciphertext is truncated" }
        val keyBytes = key.asBytes()
        val encryptedSize = payload.ciphertext.size - AesGcmPayload.TAG_SIZE_BYTES
        val encrypted = payload.ciphertext.copyOfRange(0, encryptedSize)
        val tag = payload.ciphertext.copyOfRange(encryptedSize, payload.ciphertext.size)
        val plaintext = ByteArray(encryptedSize)
        try {
            val status = keyBytes.usePointer { keyPtr ->
                payload.nonce.usePointer { noncePtr ->
                    encrypted.usePointer { encryptedPtr ->
                        plaintext.usePointer { plaintextPtr ->
                            tag.usePinned { tagPinned ->
                                CCCryptorGCMOneshotDecrypt(
                                    kCCAlgorithmAES,
                                    keyPtr,
                                    keyBytes.size.convert(),
                                    noncePtr,
                                    payload.nonce.size.convert(),
                                    null,
                                    0.convert(),
                                    encryptedPtr,
                                    encrypted.size.convert(),
                                    plaintextPtr,
                                    tagPinned.addressOf(0),
                                    tag.size.convert(),
                                )
                            }
                        }
                    }
                }
            }
            check(status == kCCSuccess) { "AES-GCM decrypt failed: $status" }
            return plaintext
        } finally {
            keyBytes.fill(0)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private inline fun <R> ByteArray.usePointer(block: (CPointer<ByteVar>?) -> R): R {
    if (isEmpty()) return block(null)
    return usePinned { block(it.addressOf(0)) }
}
