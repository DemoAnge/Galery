package com.example.galeria.features.gallery.domain

import com.example.galeria.features.gallery.data.MediaStoreScanner

actual fun createMediaRepository(): MediaRepository = MediaStoreScanner()
