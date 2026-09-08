package com.example.galeria.features.gallery.domain

interface MediaRepository {
    fun scan(): List<MediaItem>
    fun readBytes(publicUri: String): ByteArray
    fun saveJpeg(displayName: String, bytes: ByteArray)
    fun delete(publicUri: String)

    fun deleteAll(publicUris: List<String>) {
        publicUris.forEach(::delete)
    }
}

expect fun createMediaRepository(): MediaRepository
