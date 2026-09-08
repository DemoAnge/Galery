package com.example.galeria.features.vault.presentation

import com.example.galeria.core.securestorage.requireValidSecretId
import com.example.galeria.ui.playableVideoExtension
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual fun writeTempPlayableVideo(
    itemId: String,
    displayName: String,
    bytes: ByteArray,
): String {
    requireValidSecretId(itemId)
    val dir = NSTemporaryDirectory() + PLAY_DIR + "/"
    NSFileManager.defaultManager.createDirectoryAtPath(
        dir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
    val path = dir + itemId + playableVideoExtension(displayName)
    val data = bytes.toNSData()
    val ok = data.writeToFile(path, atomically = true)
    check(ok) { "Vault playback write failed" }
    return path
}

actual fun deleteTempPlayableVideo(path: String) {
    if (path.isBlank()) return
    NSFileManager.defaultManager.removeItemAtPath(path, error = null)
}

private const val PLAY_DIR = "galeria-vault-play"

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
