package com.example.galeria.features.gallery.presentation

expect class MediaPermission() {
    fun hasAccess(): Boolean
    fun request(onResult: (Boolean) -> Unit)
}
