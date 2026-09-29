package com.example.galeria.features.vault.data

import androidx.core.net.toUri
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.encryption.AesGcmEncryptor
import com.example.galeria.core.encryption.DerivedKey
import com.example.galeria.core.securestorage.requireValidSecretId
import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.ui.playableVideoExtension
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.min

actual fun encryptPublicMediaToVault(
    itemId: String,
    publicUri: String,
    key: DerivedKey,
) {
    requireValidSecretId(itemId)
    val resolver = AndroidAppContext.require().contentResolver
    val input = resolver.openInputStream(publicUri.toUri()) ?: error("Public media read failed")
    val dir = vaultFilesDir()
    dir.mkdirs()
    ensureNoMedia(dir)
    val tmp = File(dir, "$itemId.tmp")
    val target = File(dir, "$itemId.bin")
    try {
        input.use { stream ->
            tmp.outputStream().buffered().use { output ->
                AesGcmEncryptor().encryptStream(stream, output, key)
            }
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
        lockDownFile(target)
    } catch (error: Throwable) {
        tmp.delete()
        target.delete()
        throw error
    }
}

actual fun decryptVaultToPlayablePath(
    itemId: String,
    displayName: String,
    key: DerivedKey,
): String {
    requireValidSecretId(itemId)
    val src = File(vaultFilesDir(), "$itemId.bin")
    check(src.isFile) { "Vault file not found" }
    val dir = playDir()
    dir.mkdirs()
    ensureNoMedia(dir)
    val dest = File(dir, itemId + playableVideoExtension(displayName))
    src.inputStream().buffered().use { input ->
        dest.outputStream().buffered().use { output ->
            AesGcmEncryptor().decryptStream(input, output, key)
        }
    }
    dest.setReadable(false, false)
    dest.setWritable(false, false)
    dest.setReadable(true, true)
    dest.setWritable(true, true)
    return dest.absolutePath
}

actual fun decryptVaultToPlaintext(
    itemId: String,
    key: DerivedKey,
): ByteArray {
    requireValidSecretId(itemId)
    val src = File(vaultFilesDir(), "$itemId.bin")
    check(src.isFile) { "Vault file not found" }
    return src.inputStream().buffered().use { input ->
        val output = java.io.ByteArrayOutputStream(src.length().coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        AesGcmEncryptor().decryptStream(input, output, key)
        output.toByteArray()
    }
}

actual fun exportDecryptedVaultItem(
    item: VaultItem,
    key: DerivedKey,
) {
    val path = decryptVaultToPlayablePath(item.id.value, item.displayName, key)
    try {
        PublicMediaInserter().insertFromPath(item, path)
    } finally {
        deleteTempPlayableAt(path)
    }
}

private fun vaultFilesDir(): File =
    File(AndroidAppContext.require().noBackupFilesDir, VAULT_FILES_DIR)

private fun playDir(): File =
    File(AndroidAppContext.require().noBackupFilesDir, VAULT_PLAY_DIR)

private fun ensureNoMedia(dir: File) {
    val nomedia = File(dir, ".nomedia")
    if (!nomedia.exists()) nomedia.createNewFile()
}

private fun lockDownFile(file: File) {
    file.setReadable(false, false)
    file.setWritable(false, false)
    file.setExecutable(false, false)
    file.setReadable(true, true)
    file.setWritable(true, true)
}

private fun deleteTempPlayableAt(path: String) {
    if (path.isBlank()) return
    val file = File(path)
    if (!file.isFile) return
    runCatching {
        RandomAccessFile(file, "rw").use { raf ->
            val zeros = ByteArray(8192)
            var left = raf.length()
            raf.seek(0)
            while (left > 0) {
                val n = min(zeros.size.toLong(), left).toInt()
                raf.write(zeros, 0, n)
                left -= n
            }
            raf.fd.sync()
        }
        file.delete()
    }
}

internal const val VAULT_FILES_DIR = "vault"
internal const val VAULT_PLAY_DIR = "vault-play"
