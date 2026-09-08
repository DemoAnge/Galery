package com.example.galeria.features.vault.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.example.galeria.features.vault.data.looksLikeStillImage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

actual fun decodeVaultImage(bytes: ByteArray): ImageBitmap? {
    return try {
        if (looksLikeStillImage(bytes)) {
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } else {
            decodeVaultVideoFrame(bytes)
        }
    } catch (_: Throwable) {
        decodeVaultVideoFrame(bytes)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun decodeVaultVideoFrame(bytes: ByteArray): ImageBitmap? {
    val path = NSTemporaryDirectory() + "vault-thumb-${bytes.size}.mp4"
    val data = bytes.toNSData()
    if (!data.writeToFile(path, atomically = true)) return null
    return try {
        val url = NSURL.fileURLWithPath(path)
        val asset = AVURLAsset.URLAssetWithURL(url, options = null)
        val generator = AVAssetImageGenerator(asset = asset)
        generator.appliesPreferredTrackTransform = true
        val cgImage = generator.copyCGImageAtTime(CMTimeMake(0, 1), actualTime = null, error = null)
            ?: return null
        val uiImage = UIImage.imageWithCGImage(cgImage)
        val jpeg = UIImageJPEGRepresentation(uiImage, 0.82) ?: return null
        Image.makeFromEncoded(jpeg.toByteArray()).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    } finally {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }
}

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
        platform.posix.memcpy(pinned.addressOf(0), bytes, this.length)
    }
    return out
}
