package com.example.galeria.features.gallery.data

import com.example.galeria.features.gallery.domain.UserCollection
import platform.Foundation.NSUserDefaults

actual class CollectionStore actual constructor() {
    actual fun load(): List<UserCollection> {
        val text = NSUserDefaults.standardUserDefaults.stringForKey(KEY).orEmpty()
        return UserCollectionCodec.decode(text)
    }

    actual fun save(collections: List<UserCollection>) {
        NSUserDefaults.standardUserDefaults.setObject(
            UserCollectionCodec.encode(collections),
            forKey = KEY,
        )
    }
}

private const val KEY = "galeria.user_collections"
