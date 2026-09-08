package com.example.galeria.features.gallery.data

import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.features.gallery.domain.MediaRepository
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
import platform.Foundation.NSMutableData
import platform.Foundation.NSSortDescriptor
import platform.Foundation.create
import platform.Photos.PHAsset
import platform.Photos.PHAssetCollection
import platform.Photos.PHAssetCollectionSubtypeAlbumRegular
import platform.Photos.PHAssetCollectionSubtypeSmartAlbumScreenshots
import platform.Photos.PHAssetCollectionTypeAlbum
import platform.Photos.PHAssetCollectionTypeSmartAlbum
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetMediaSubtypePhotoScreenshot
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetMediaTypeVideo
import platform.Photos.PHAssetResource
import platform.Photos.PHAssetResourceManager
import platform.Photos.PHAssetResourceRequestOptions
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHFetchOptions
import platform.Photos.PHPhotoLibrary
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class PhotoLibraryScanner : MediaRepository {

    override fun scan(): List<MediaItem> {
        requirePhotosAccess()
        val named = namedAlbumHints()
        val screenshotIds = screenshotAssetIds()
        val options = PHFetchOptions()
        options.sortDescriptors = listOf(NSSortDescriptor("creationDate", false))
        val fetch = PHAsset.fetchAssetsWithOptions(options)
        val items = ArrayList<MediaItem>(fetch.count.toInt())
        repeat(fetch.count.toInt()) { index ->
            val asset = fetch.objectAtIndex(index.toULong()) as? PHAsset ?: return@repeat
            val kind = when (asset.mediaType) {
                PHAssetMediaTypeImage -> GalleryMediaKind.IMAGE
                PHAssetMediaTypeVideo -> GalleryMediaKind.VIDEO
                else -> return@repeat
            }
            val id = asset.localIdentifier
            val isScreenshot = screenshotIds.contains(id) || asset.isScreenshot()
            val hint = when {
                isScreenshot -> "Screenshots"
                else -> named[id] ?: "DCIM/Camera"
            }
            val date = ((asset.creationDate?.timeIntervalSince1970 ?: 0.0) * 1000.0).toLong()
            val resource = PHAssetResource.assetResourcesForAsset(asset).firstOrNull() as? PHAssetResource
            val displayName = resource?.originalFilename ?: "media"
            items += MediaItem(
                id = id,
                publicUri = id,
                displayName = displayName,
                mediaKind = kind,
                capturedAtEpochMs = date,
                sizeBytes = 0L,
                relativePath = hint,
                bucketName = hint,
                widthPx = asset.pixelWidth.toInt(),
                heightPx = asset.pixelHeight.toInt(),
                durationMs = (asset.duration * 1000.0).toLong(),
            )
        }
        return items
    }

    private fun screenshotAssetIds(): Set<String> {
        val collections = PHAssetCollection.fetchAssetCollectionsWithType(
            PHAssetCollectionTypeSmartAlbum,
            PHAssetCollectionSubtypeSmartAlbumScreenshots,
            null,
        )
        return assetIdsIn(collections)
    }

    private fun namedAlbumHints(): Map<String, String> {
        val collections = PHAssetCollection.fetchAssetCollectionsWithType(
            PHAssetCollectionTypeAlbum,
            PHAssetCollectionSubtypeAlbumRegular,
            null,
        )
        val result = HashMap<String, String>()
        repeat(collections.count.toInt()) { index ->
            val collection = collections.objectAtIndex(index.toULong()) as? PHAssetCollection ?: return@repeat
            val title = collection.localizedTitle ?: return@repeat
            val assets = PHAsset.fetchAssetsInAssetCollection(collection, null)
            repeat(assets.count.toInt()) { assetIndex ->
                val asset = assets.objectAtIndex(assetIndex.toULong()) as? PHAsset ?: return@repeat
                result[asset.localIdentifier] = title
            }
        }
        return result
    }

    private fun assetIdsIn(collections: platform.Photos.PHFetchResult): Set<String> {
        val ids = HashSet<String>()
        repeat(collections.count.toInt()) { index ->
            val collection = collections.objectAtIndex(index.toULong()) as? PHAssetCollection ?: return@repeat
            val assets = PHAsset.fetchAssetsInAssetCollection(collection, null)
            repeat(assets.count.toInt()) { assetIndex ->
                val asset = assets.objectAtIndex(assetIndex.toULong()) as? PHAsset ?: return@repeat
                ids += asset.localIdentifier
            }
        }
        return ids
    }

    override fun readBytes(publicUri: String): ByteArray {
        requirePhotosAccess()
        val fetch = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(publicUri), null)
        val asset = fetch.firstObject as? PHAsset ?: error("Public media read failed")
        val resources = PHAssetResource.assetResourcesForAsset(asset)
        val resource = resources.firstOrNull() as? PHAssetResource ?: error("Public media read failed")
        val collected = NSMutableData()
        val semaphore = dispatch_semaphore_create(0)
        var failed = false
        val options = PHAssetResourceRequestOptions().apply { setNetworkAccessAllowed(true) }
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
        check(!failed && collected.length.toInt() > 0) { "Public media read failed" }
        return collected.toByteArray()
    }

    override fun delete(publicUri: String) {
        requirePhotosAccess()
        val fetch = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(publicUri), null)
        val asset = fetch.firstObject as? PHAsset ?: error("Public media delete failed")
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            val ok = PHPhotoLibrary.sharedPhotoLibrary().performChangesAndWait(
                {
                    PHAssetChangeRequest.deleteAssets(listOf(asset))
                },
                errorVar.ptr,
            )
            check(ok && errorVar.value == null) { "Public media delete failed" }
        }
    }

    override fun saveJpeg(displayName: String, bytes: ByteArray) {
        requirePhotosAccess()
        val data = bytes.toNSData()
        memScoped {
            val errorVar = alloc<ObjCObjectVar<NSError?>>()
            val ok = PHPhotoLibrary.sharedPhotoLibrary().performChangesAndWait(
                {
                    val request = PHAssetCreationRequest.creationRequestForAsset()
                    request.addResourceWithType(PHAssetResourceTypePhoto, data = data, options = null)
                },
                errorVar.ptr,
            )
            check(ok && errorVar.value == null) { "Public media insert failed" }
        }
    }
}

private fun PHAsset.isScreenshot(): Boolean {
    val subtype = mediaSubtypes
    return (subtype.toULong() and PHAssetMediaSubtypePhotoScreenshot.toULong()) != 0UL
}

private fun requirePhotosAccess() {
    val status = PHPhotoLibrary.authorizationStatus()
    check(
        status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited,
    ) { "Photos access not granted" }
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
        memcpy(pinned.addressOf(0), bytes, this.length)
    }
    return out
}
