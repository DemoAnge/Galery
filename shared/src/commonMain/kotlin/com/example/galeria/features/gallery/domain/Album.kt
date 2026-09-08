package com.example.galeria.features.gallery.domain

enum class AlbumId {
    CAMERA,
    SCREENSHOTS,
    WHATSAPP,
    DOWNLOADS,
    OTHER,
}

fun AlbumId.title(): String = when (this) {
    AlbumId.CAMERA -> "Cámara"
    AlbumId.SCREENSHOTS -> "Capturas"
    AlbumId.WHATSAPP -> "WhatsApp"
    AlbumId.DOWNLOADS -> "Descargas"
    AlbumId.OTHER -> "Otros"
}

data class Album(
    val id: AlbumId,
    val title: String,
    val items: List<MediaItem>,
    val customId: String? = null,
)
