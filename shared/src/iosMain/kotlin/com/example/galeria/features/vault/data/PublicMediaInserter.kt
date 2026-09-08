package com.example.galeria.features.vault.data

import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultMediaKind
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.create
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAssetResourceTypeVideo
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary

@OptIn(ExperimentalForeignApi::class)
actual class PublicMediaInserter actual constructor() {

    actual fun insert(item: VaultItem, plaintext: ByteArray) {
        val status = PHPhotoLibrary.authorizationStatus()
        check(
            status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited,
        ) { "Photos access not granted" }
        val data = plaintext.toNSData()
        val resourceType = if (item.mediaKind == VaultMediaKind.IMAGE) {
            PHAssetResourceTypePhoto
        } else {
            PHAssetResourceTypeVideo
        }
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            val ok = PHPhotoLibrary.sharedPhotoLibrary().performChangesAndWait(
                {
                    val request = PHAssetCreationRequest.creationRequestForAsset()
                    request.addResourceWithType(resourceType, data = data, options = null)
                },
                errorVar.ptr,
            )
            check(ok && errorVar.value == null) { "Public media insert failed" }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
