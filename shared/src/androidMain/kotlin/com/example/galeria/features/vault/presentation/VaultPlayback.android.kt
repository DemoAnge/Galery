package com.example.galeria.features.vault.presentation

import com.example.galeria.core.AndroidAppContext
import com.example.galeria.core.securestorage.requireValidSecretId
import com.example.galeria.ui.playableVideoExtension
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.min

actual fun writeTempPlayableVideo(
    itemId: String,
    displayName: String,
    bytes: ByteArray,
): String {
    requireValidSecretId(itemId)
    val dir = File(AndroidAppContext.require().noBackupFilesDir, PLAY_DIR)
    dir.mkdirs()
    val nomedia = File(dir, ".nomedia")
    if (!nomedia.exists()) nomedia.createNewFile()
    val file = File(dir, itemId + playableVideoExtension(displayName))
    file.writeBytes(bytes)
    file.setReadable(false, false)
    file.setWritable(false, false)
    file.setReadable(true, true)
    file.setWritable(true, true)
    return file.absolutePath
}

actual fun deleteTempPlayableVideo(path: String) {
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

private const val PLAY_DIR = "vault-play"
