package com.example.galeria.features.gallery.data

expect class FavoritesStore() {
    fun load(): Set<String>
    fun save(ids: Set<String>)
}
