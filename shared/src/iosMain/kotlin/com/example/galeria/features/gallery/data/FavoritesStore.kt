package com.example.galeria.features.gallery.data

import platform.Foundation.NSUserDefaults

actual class FavoritesStore actual constructor() {
    actual fun load(): Set<String> {
        val text = NSUserDefaults.standardUserDefaults.stringForKey(KEY).orEmpty()
        return FavoritesCodec.decode(text)
    }

    actual fun save(ids: Set<String>) {
        NSUserDefaults.standardUserDefaults.setObject(
            FavoritesCodec.encode(ids),
            forKey = KEY,
        )
    }
}

private const val KEY = "galeria.favorites"
