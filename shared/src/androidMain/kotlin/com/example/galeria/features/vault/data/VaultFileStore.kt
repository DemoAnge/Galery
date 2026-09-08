package com.example.galeria.features.vault.data

import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.encryption.AesGcmPayload
import com.example.galeria.core.securestorage.requireValidSecretId
import java.io.File

actual class VaultFileStore actual constructor() {

    private val dir: File =
        File(AndroidAppContext.require().noBackupFilesDir, DIR)

    actual fun write(itemId: String, payload: AesGcmPayload) {
        requireValidSecretId(itemId)
        ensureDir()
        val target = fileFor(itemId)
        val tmp = File(dir, "$itemId.tmp")
        val bytes = payload.toFileBytes()
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            target.writeBytes(bytes)
            tmp.delete()
        }
        lockDown(target)
    }

    actual fun read(itemId: String): AesGcmPayload {
        requireValidSecretId(itemId)
        val file = fileFor(itemId)
        check(file.exists()) { "Vault file not found" }
        return file.readBytes().toAesGcmPayload()
    }

    actual fun delete(itemId: String) {
        requireValidSecretId(itemId)
        fileFor(itemId).delete()
    }

    actual fun exists(itemId: String): Boolean {
        requireValidSecretId(itemId)
        return fileFor(itemId).exists()
    }

    private fun fileFor(itemId: String): File = File(dir, "$itemId.bin")

    private fun ensureDir() {
        dir.mkdirs()
        lockDown(dir)
        val nomedia = File(dir, NOMEDIA)
        if (!nomedia.exists()) {
            nomedia.createNewFile()
        }
        lockDown(nomedia)
    }

    private fun lockDown(file: File) {
        file.setReadable(false, false)
        file.setWritable(false, false)
        file.setExecutable(false, false)
        file.setReadable(true, true)
        file.setWritable(true, true)
        file.setExecutable(file.isDirectory, true)
    }

    private companion object {
        const val DIR = "vault"
        const val NOMEDIA = ".nomedia"
    }
}
