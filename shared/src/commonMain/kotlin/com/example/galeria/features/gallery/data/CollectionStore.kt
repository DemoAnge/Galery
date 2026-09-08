package com.example.galeria.features.gallery.data

import com.example.galeria.features.gallery.domain.UserCollection

expect class CollectionStore() {
    fun load(): List<UserCollection>
    fun save(collections: List<UserCollection>)
}
