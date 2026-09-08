package com.example.galeria.features.vault.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSMutableArray
import platform.Foundation.NSMutableData
import platform.Photos.PHAsset
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAssetResource
import platform.Photos.PHAssetResourceManager
import platform.Photos.PHAssetResourceRequestOptions
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.posix.memcpy
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual class PublicMediaRemover actual constructor() {

    actual fun readBytes(publicUri: String): ByteArray {
        requirePhotosAccess()
        val asset = fetchAsset(publicUri)
        val resources = PHAssetResource.assetResourcesForAsset(asset)
        val resource = resources.firstOrNull() as? PHAssetResource
            ?: error("Public media read failed")
        val collected = NSMutableData()
        val semaphore = dispatch_semaphore_create(0)
        var failed = false
        val options = PHAssetResourceRequestOptions().apply {
            setNetworkAccessAllowed(true)
        }
        PHAssetResourceManager.defaultManager.requestDataForAssetResource(
            resource,
            options = options,
            dataReceivedHandler = { chunk ->
                val data = chunk as? NSData
                if (data != null) collected.appendData(data)
            },
            completionHandler = { error ->
                if (error != null) failed = true
                dispatch_semaphore_signal(semaphore)
            },
        )
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
        check(!failed) { "Public media read failed" }
        return collected.toByteArray()
    }

    actual fun delete(publicUri: String) {
        requirePhotosAccess()
        val asset = fetchAsset(publicUri)
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            val assets = NSMutableArray()
            assets.addObject(asset)
            val ok = PHPhotoLibrary.sharedPhotoLibrary().performChangesAndWait(
                {
                    PHAssetChangeRequest.deleteAssets(assets)
                },
                errorVar.ptr,
            )
            check(ok && errorVar.value == null) { "Public media delete failed" }
        }
    }

    actual fun deleteAll(publicUris: List<String>) {
        if (publicUris.isEmpty()) return
        requirePhotosAccess()
        val assets = NSMutableArray()
        publicUris.forEach { uri ->
            assets.addObject(fetchAsset(uri))
        }
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            val ok = PHPhotoLibrary.sharedPhotoLibrary().performChangesAndWait(
                {
                    PHAssetChangeRequest.deleteAssets(assets)
                },
                errorVar.ptr,
            )
            check(ok && errorVar.value == null) { "Public media delete failed" }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun fetchAsset(publicUri: String): PHAsset {
    val fetch = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(publicUri), null)
    return fetch.firstObject as? PHAsset ?: error("Public media not found")
}

private fun requirePhotosAccess() {
    val status = PHPhotoLibrary.authorizationStatus()
    check(
        status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited,
    ) { "Photos access not granted" }
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
