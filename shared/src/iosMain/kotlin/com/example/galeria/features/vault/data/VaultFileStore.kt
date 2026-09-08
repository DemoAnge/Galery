package com.example.galeria.features.vault.data

import com.example.galeria.core.encryption.AesGcmPayload
import com.example.galeria.core.securestorage.requireValidSecretId
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSNumber
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.numberWithBool
import platform.Foundation.writeToURL
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class VaultFileStore actual constructor() {

    private val dir: NSURL = resolveVaultDir()

    actual fun write(itemId: String, payload: AesGcmPayload) {
        requireValidSecretId(itemId)
        val url = fileUrl(itemId)
        val data = payload.toFileBytes().toNSData()
        val ok = data.writeToURL(url, atomically = true)
        check(ok) { "Vault file write failed" }
    }

    actual fun read(itemId: String): AesGcmPayload {
        requireValidSecretId(itemId)
        val data = NSData.dataWithContentsOfURL(fileUrl(itemId)) ?: error("Vault file not found")
        return data.toByteArray().toAesGcmPayload()
    }

    actual fun delete(itemId: String) {
        requireValidSecretId(itemId)
        NSFileManager.defaultManager.removeItemAtURL(fileUrl(itemId), null)
    }

    actual fun exists(itemId: String): Boolean {
        requireValidSecretId(itemId)
        return NSFileManager.defaultManager.fileExistsAtPath(fileUrl(itemId).path ?: return false)
    }

    private fun fileUrl(itemId: String): NSURL =
        dir.URLByAppendingPathComponent("$itemId.bin")
            ?: error("Vault file path failed")
}

@OptIn(ExperimentalForeignApi::class)
private fun resolveVaultDir(): NSURL {
    val manager = NSFileManager.defaultManager
    val base = manager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    ) ?: error("Vault directory unavailable")
    val dir = base.URLByAppendingPathComponent(VAULT_DIR) ?: error("Vault directory unavailable")
    if (!manager.fileExistsAtPath(dir.path ?: "")) {
        manager.createDirectoryAtURL(
            url = dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
    dir.setResourceValue(
        NSNumber.numberWithBool(true),
        forKey = NSURLIsExcludedFromBackupKey,
        error = null,
    )
    return dir
}

private const val VAULT_DIR = "galeria-vault"

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
