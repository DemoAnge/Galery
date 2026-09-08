package com.example.galeria.features.gallery.domain

import com.example.galeria.features.gallery.data.PhotoLibraryScanner

actual fun createMediaRepository(): MediaRepository = PhotoLibraryScanner()
