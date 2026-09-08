package com.example.galeria.core.securestorage

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.darwin.OSStatus
import platform.posix.memcpy

/**
 * Secretos en Keychain, accesibles con el dispositivo desbloqueado y
 * excluidos de backup/migración (`WhenUnlockedThisDeviceOnly`).
 */
@OptIn(ExperimentalForeignApi::class)
class IosSecureKeyStore : SecureKeyStore {

    override fun write(id: String, secret: ByteArray) {
        requireValidSecretId(id)
        delete(id)
        val data = secret.toNSData()
        val status = withQuery(id) { dict ->
            dict.put(kSecValueData, data)
            dict.put(kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
            SecItemAdd(dict.asCfDictionary(), null)
        }
        check(status == errSecSuccess) { "Keychain write failed: $status" }
    }

    override fun read(id: String): ByteArray? {
        requireValidSecretId(id)
        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = withQuery(id) { dict ->
                dict.put(kSecReturnData, kCFBooleanTrue)
                dict.put(kSecMatchLimit, kSecMatchLimitOne)
                SecItemCopyMatching(dict.asCfDictionary(), result.ptr)
            }
            if (status == errSecItemNotFound) return@memScoped null
            check(status == errSecSuccess) { "Keychain read failed: $status" }
            (result.value as NSData).toByteArray()
        }
    }

    override fun delete(id: String) {
        requireValidSecretId(id)
        val status = withQuery(id) { dict ->
            SecItemDelete(dict.asCfDictionary())
        }
        check(status == errSecSuccess || status == errSecItemNotFound) {
            "Keychain delete failed: $status"
        }
    }

    override fun contains(id: String): Boolean {
        requireValidSecretId(id)
        val status = withQuery(id) { dict ->
            dict.put(kSecMatchLimit, kSecMatchLimitOne)
            SecItemCopyMatching(dict.asCfDictionary(), null)
        }
        return when (status) {
            errSecSuccess -> true
            errSecItemNotFound -> false
            else -> error("Keychain contains failed: $status")
        }
    }
}

private const val SERVICE = "com.example.galeria.securestore"

@OptIn(ExperimentalForeignApi::class)
private inline fun withQuery(
    id: String,
    block: (NSMutableDictionary) -> OSStatus,
): OSStatus {
    val dict = NSMutableDictionary()
    dict.put(kSecClass, kSecClassGenericPassword)
    dict.put(kSecAttrService, SERVICE)
    dict.put(kSecAttrAccount, id)
    return block(dict)
}

private fun NSMutableDictionary.put(key: Any?, value: Any?) {
    if (key != null && value != null) {
        setObject(value, forKey = key as NSCopyingProtocol)
    }
}

@Suppress("UNCHECKED_CAST")
private fun NSMutableDictionary.asCfDictionary(): CFDictionaryRef = this as CFDictionaryRef

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, this.length)
    }
    return out
}
