package com.example.galeria.features.vault.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.galeria.features.vault.data.looksLikeStillImage
import com.example.galeria.features.vault.data.sampledBitmapFromBytes
import com.example.galeria.features.vault.data.vaultVideoFrameBitmap

actual fun decodeVaultImage(bytes: ByteArray): ImageBitmap? {
    return try {
        if (looksLikeStillImage(bytes)) {
            sampledBitmapFromBytes(bytes, 1920)?.asImageBitmap()
        } else {
            vaultVideoFrameBitmap(bytes, "clip.mp4", 720)?.asImageBitmap()
        }
    } catch (_: Throwable) {
        null
    }
}
