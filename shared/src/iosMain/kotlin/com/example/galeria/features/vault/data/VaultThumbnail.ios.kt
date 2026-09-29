package com.example.galeria.features.vault.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVURLAsset
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal actual fun encodeVaultThumbnailFromPublicUri(
    publicUri: String,
    isVideo: Boolean,
    displayName: String,
): ByteArray? {
    return try {
        val fetch = platform.Photos.PHAsset.fetchAssetsWithLocalIdentifiers(listOf(publicUri), null)
        val asset = fetch.firstObject as? platform.Photos.PHAsset ?: return null
        val options = platform.Photos.PHImageRequestOptions().apply {
            setSynchronous(true)
            setNetworkAccessAllowed(true)
            setResizeMode(platform.Photos.PHImageRequestOptionsResizeModeExact)
            setDeliveryMode(platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat)
        }
        val size = VAULT_THUMB_MAX_EDGE.toDouble()
        val semaphore = platform.darwin.dispatch_semaphore_create(0)
        var jpeg: ByteArray? = null
        platform.Photos.PHImageManager.defaultManager.requestImageForAsset(
            asset,
            targetSize = CGSizeMake(size, size),
            contentMode = platform.Photos.PHImageContentModeAspectFit,
            options = options,
        ) { image, _ ->
            val uiImage = image as? UIImage
            jpeg = uiImage?.let { jpegBytes(it) }
            platform.darwin.dispatch_semaphore_signal(semaphore)
        }
        platform.darwin.dispatch_semaphore_wait(semaphore, platform.darwin.DISPATCH_TIME_FOREVER)
        jpeg
    } catch (_: Throwable) {
        null
    }
}

internal actual fun encodeVaultThumbnailJpeg(
    plaintext: ByteArray,
    isVideo: Boolean,
    displayName: String,
): ByteArray? {
    return try {
        if (!isVideo && looksLikeStillImage(plaintext)) {
            val data = plaintext.toThumbNSData()
            val image = UIImage.imageWithData(data) ?: return null
            jpegBytes(image)
        } else {
            videoJpeg(plaintext, displayName)
        }
    } catch (_: Throwable) {
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun videoJpeg(bytes: ByteArray, displayName: String): ByteArray? {
    val ext = when {
        displayName.lowercase().endsWith(".mov") -> ".mov"
        displayName.lowercase().endsWith(".m4v") -> ".m4v"
        else -> ".mp4"
    }
    val path = NSTemporaryDirectory() + "vault-thumb-$ext"
    val data = bytes.toThumbNSData()
    if (!data.writeToFile(path, atomically = true)) return null
    return try {
        val url = NSURL.fileURLWithPath(path)
        val asset = AVURLAsset.URLAssetWithURL(url, options = null)
        val generator = AVAssetImageGenerator(asset = asset)
        generator.appliesPreferredTrackTransform = true
        generator.maximumSize = CGSizeMake(VAULT_THUMB_MAX_EDGE.toDouble(), VAULT_THUMB_MAX_EDGE.toDouble())
        val cgImage = generator.copyCGImageAtTime(CMTimeMake(0, 1), actualTime = null, error = null)
            ?: return null
        val uiImage = UIImage.imageWithCGImage(cgImage)
        jpegBytes(uiImage)
    } catch (_: Throwable) {
        null
    } finally {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun jpegBytes(image: UIImage): ByteArray? {
    val jpeg = UIImageJPEGRepresentation(image, 0.75) ?: return null
    return jpeg.toThumbByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toThumbNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toThumbByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, this.length)
    }
    return out
}
