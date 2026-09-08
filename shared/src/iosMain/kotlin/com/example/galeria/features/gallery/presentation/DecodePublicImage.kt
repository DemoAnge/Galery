package com.example.galeria.features.gallery.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Photos.PHAsset
import platform.Photos.PHImageContentModeAspectFit
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeExact
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual fun decodePublicImage(publicUri: String, maxEdge: Int): ImageBitmap? {
    val bytes = loadEditJpeg(publicUri, maxEdge) ?: return null
    return try {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun loadEditJpeg(publicUri: String, maxEdge: Int): ByteArray? {
    val fetch = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(publicUri), null)
    val asset = fetch.firstObject as? PHAsset ?: return null
    val options = PHImageRequestOptions().apply {
        setSynchronous(true)
        setNetworkAccessAllowed(true)
        setResizeMode(PHImageRequestOptionsResizeModeExact)
        setDeliveryMode(PHImageRequestOptionsDeliveryModeHighQualityFormat)
    }
    val size = maxEdge.coerceAtLeast(64).toDouble()
    val semaphore = dispatch_semaphore_create(0)
    var jpeg: ByteArray? = null
    PHImageManager.defaultManager.requestImageForAsset(
        asset,
        targetSize = CGSizeMake(size, size),
        contentMode = PHImageContentModeAspectFit,
        options = options,
    ) { image, _ ->
        val uiImage = image as? UIImage
        val data = uiImage?.let { UIImageJPEGRepresentation(it, 0.9) }
        jpeg = data?.toByteArray()
        dispatch_semaphore_signal(semaphore)
    }
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
    return jpeg
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
