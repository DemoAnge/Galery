package com.example.galeria.features.vault.presentation

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
import platform.Foundation.NSError
import platform.Foundation.NSMutableDictionary
import platform.Foundation.create
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAccessControlBiometryCurrentSet
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecUseOperationPrompt
import platform.Security.kSecValueData
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class BiometricUnlock actual constructor() {

    actual fun isAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
    }

    actual fun wrap(dek: ByteArray, onResult: (ByteArray?) -> Unit) {
        val context = LAContext()
        context.evaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = "Activa el atajo biométrico del Vault",
        ) { success, _ ->
            if (!success) {
                onResult(null)
                return@evaluatePolicy
            }
            val stored = storeDek(dek)
            onResult(if (stored) TOKEN else null)
        }
    }

    actual fun unwrap(wrapped: ByteArray, onResult: (ByteArray?) -> Unit) {
        val dek = readDek()
        onResult(dek)
    }

    actual fun cancel() {
    }

    private fun storeDek(dek: ByteArray): Boolean {
        deleteDek()
        val access = SecAccessControlCreateWithFlags(
            null,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            kSecAccessControlBiometryCurrentSet,
            null,
        ) ?: return false
        val data = dek.toNSData()
        val dict = NSMutableDictionary()
        dict.put(kSecClass, kSecClassGenericPassword)
        dict.put(kSecAttrService, SERVICE)
        dict.put(kSecAttrAccount, ACCOUNT)
        dict.put(kSecValueData, data)
        dict.put(kSecAttrAccessControl, access)
        val status = SecItemAdd(dict.asCfDictionary(), null)
        return status == errSecSuccess
    }

    private fun readDek(): ByteArray? {
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val dict = NSMutableDictionary()
            dict.put(kSecClass, kSecClassGenericPassword)
            dict.put(kSecAttrService, SERVICE)
            dict.put(kSecAttrAccount, ACCOUNT)
            dict.put(kSecReturnData, kCFBooleanTrue)
            dict.put(kSecMatchLimit, kSecMatchLimitOne)
            dict.put(kSecUseOperationPrompt, "Desbloquear Vault")
            val status = SecItemCopyMatching(dict.asCfDictionary(), result.ptr)
            if (status == errSecItemNotFound || status != errSecSuccess) return null
            return (result.value as NSData).toByteArray()
        }
    }

    private fun deleteDek() {
        val dict = NSMutableDictionary()
        dict.put(kSecClass, kSecClassGenericPassword)
        dict.put(kSecAttrService, SERVICE)
        dict.put(kSecAttrAccount, ACCOUNT)
        SecItemDelete(dict.asCfDictionary())
    }
}

private const val SERVICE = "com.example.galeria.vault.bio"
private const val ACCOUNT = "dek"
private val TOKEN = byteArrayOf(0x42)

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
