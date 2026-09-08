@file:OptIn(ExperimentalForeignApi::class)

package com.example.galeria.features.gallery.domain

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Photos.PHChange
import platform.Photos.PHPhotoLibrary
import platform.Photos.PHPhotoLibraryChangeObserverProtocol
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol

actual class MediaLibraryWatcher actual constructor() {
    private var photoObserver: PhotoLibraryChangeRelay? = null
    private var resumeObserver: NSObjectProtocol? = null
    private var ignoreNextResume = true

    actual fun start(onChanged: () -> Unit) {
        stop()
        ignoreNextResume = true
        val photo = PhotoLibraryChangeRelay(onChanged)
        photoObserver = photo
        PHPhotoLibrary.sharedPhotoLibrary().registerChangeObserver(photo)
        resumeObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null,
        ) { _ ->
            if (ignoreNextResume) {
                ignoreNextResume = false
            } else {
                onChanged()
            }
        }
    }

    actual fun stop() {
        photoObserver?.let { observer ->
            PHPhotoLibrary.sharedPhotoLibrary().unregisterChangeObserver(observer)
        }
        photoObserver = null
        resumeObserver?.let { token ->
            NSNotificationCenter.defaultCenter.removeObserver(token)
        }
        resumeObserver = null
        ignoreNextResume = true
    }
}

private class PhotoLibraryChangeRelay(
    private val onChanged: () -> Unit,
) : NSObject(), PHPhotoLibraryChangeObserverProtocol {
    override fun photoLibraryDidChange(changeInstance: PHChange) {
        onChanged()
    }
}
