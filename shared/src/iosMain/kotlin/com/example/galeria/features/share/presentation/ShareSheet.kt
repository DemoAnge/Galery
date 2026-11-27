package com.example.galeria.features.share.presentation

import com.example.galeria.features.share.domain.ShareKind
import com.example.galeria.features.share.domain.ShareTarget
import com.example.galeria.features.share.domain.allowsStoryShare
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
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIPasteboard
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

    actual fun targets(requests: List<ShareRequest>): List<ShareTarget> {
        check(requests.isNotEmpty()) { "Missing public media" }
        val mimeTypes = requests.map { it.mimeType }
        val storyOk = allowsStoryShare(mimeTypes)
        val storyReason = if (storyOk) null else "Elige una sola foto o video"
        return buildList {
            if (canOpen("instagram-stories://share")) {
                add(
                    ShareTarget(
                        id = ShareTarget.ID_INSTAGRAM_STORY,
                        label = "Historias IG",
                        kind = ShareKind.INSTAGRAM_STORY,
                        enabled = storyOk,
                        reason = storyReason,
                    ),
                )
            }
            if (canOpen("instagram://app")) {
                add(
                    ShareTarget(
                        id = ShareTarget.ID_INSTAGRAM,
                        label = "Instagram",
                        kind = ShareKind.INSTAGRAM,
                        enabled = mimeTypes.size == 1,
                        reason = if (mimeTypes.size == 1) null else "Instagram admite una sola foto o video",
                    ),
                )
            }
            if (canOpen("facebook-stories://share")) {
                add(
                    ShareTarget(
                        id = ShareTarget.ID_FACEBOOK_STORY,
                        label = "Historias FB",
                        kind = ShareKind.FACEBOOK_STORY,
                        enabled = storyOk,
                        reason = storyReason,
                    ),
                )
            }
            add(
                ShareTarget(
                    id = ShareTarget.ID_NEARBY,
                    label = "AirDrop",
                    kind = ShareKind.NEARBY,
                ),
            )
            add(ShareTarget.MORE)
        }
    }

    actual fun share(requests: List<ShareRequest>) {
        presentSystemSheet(requests)
    }

    actual fun shareTo(requests: List<ShareRequest>, target: ShareTarget) {
        check(requests.isNotEmpty()) { "Missing public media" }
        when (target.kind) {
            ShareKind.INSTAGRAM_STORY -> shareToStory(
                request = requests.first(),
                scheme = "instagram-stories://share",
                imageKey = "com.instagram.sharedSticker.backgroundImage",
                videoKey = "com.instagram.sharedSticker.backgroundVideo",
            )
            ShareKind.FACEBOOK_STORY -> shareToStory(
                request = requests.first(),
                scheme = "facebook-stories://share",
                imageKey = "com.facebook.sharedSticker.backgroundImage",
                videoKey = "com.facebook.sharedSticker.backgroundVideo",
            )
            else -> presentSystemSheet(requests)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun presentSystemSheet(requests: List<ShareRequest>) {
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

@OptIn(ExperimentalForeignApi::class)
private fun shareToStory(
    request: ShareRequest,
    scheme: String,
    imageKey: String,
    videoKey: String,
) {
    val item = loadShareItem(request) ?: error("Share item unavailable")
    val payload = when (item) {
        is UIImage -> {
            val data = UIImageJPEGRepresentation(item, 0.92) ?: error("Share item unavailable")
            mapOf(imageKey to data)
        }
        is NSURL -> mapOf(videoKey to item)
        else -> error("Share item unavailable")
    }
    UIPasteboard.generalPasteboard.setItems(listOf(payload))
    val url = NSURL.URLWithString(scheme) ?: error("Share target unavailable")
    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
}

private fun canOpen(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    return UIApplication.sharedApplication.canOpenURL(nsUrl)
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
