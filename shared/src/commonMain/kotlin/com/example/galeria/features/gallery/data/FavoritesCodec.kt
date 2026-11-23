package com.example.galeria.features.gallery.data

object FavoritesCodec {
    fun encode(ids: Set<String>): String =
        ids.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

    fun decode(text: String): Set<String> {
        if (text.isBlank()) return emptySet()
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
