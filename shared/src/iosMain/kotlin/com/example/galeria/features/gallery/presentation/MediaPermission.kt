package com.example.galeria.features.gallery.presentation

import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary

actual class MediaPermission actual constructor() {

    actual fun hasAccess(): Boolean {
        val status = PHPhotoLibrary.authorizationStatus()
        return status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited
    }

    actual fun request(onResult: (Boolean) -> Unit) {
        if (hasAccess()) {
            onResult(true)
            return
        }
        PHPhotoLibrary.requestAuthorization { status ->
            onResult(
                status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited,
            )
        }
    }
}
