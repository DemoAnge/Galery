package com.example.galeria.features.share.presentation

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.NSURL
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.writeToFile
import platform.Photos.PHAsset
import platform.Photos.PHAssetResource
import platform.Photos.PHAssetResourceManager
import platform.Photos.PHAssetResourceRequestOptions
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

object IosShareHost {
    var controller: UIViewController? = null
}

@OptIn(ExperimentalForeignApi::class)
actual class ShareSheet actual constructor() {

    actual fun share(requests: List<ShareRequest>) {
        val host = IosShareHost.controller ?: error("Share requires a view controller")
        check(requests.isNotEmpty()) { "Missing public media" }
        val items = requests.mapNotNull(::loadShareItem)
        check(items.isNotEmpty()) { "Share item unavailable" }
        val sheet = UIActivityViewController(
            activityItems = items,
            applicationActivities = null,
        )
        sheet.popoverPresentationController?.sourceView = host.view
        host.presentViewController(sheet, animated = true, completion = null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun loadShareItem(request: ShareRequest): Any? {
    val fetch = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(request.publicUri), null)
    val asset = fetch.firstObject as? PHAsset ?: return null
    val resources = PHAssetResource.assetResourcesForAsset(asset)
    val resource = resources.firstOrNull() as? PHAssetResource ?: return null
    val data = readResource(resource) ?: return null
    return if (request.mimeType.startsWith("image/")) {
        UIImage.imageWithData(data)
    } else {
        val name = resource.originalFilename ?: "share.bin"
        val path = NSTemporaryDirectory() + name
        data.writeToFile(path, atomically = true)
        NSURL.fileURLWithPath(path)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readResource(resource: PHAssetResource): NSData? {
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
    if (failed || collected.length.toInt() == 0) return null
    return collected
}
