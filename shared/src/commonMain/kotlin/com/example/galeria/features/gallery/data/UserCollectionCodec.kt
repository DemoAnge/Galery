package com.example.galeria.features.gallery.data

import com.example.galeria.features.gallery.domain.UserCollection

object UserCollectionCodec {
    private const val UNIT = '\u001f'
    private const val RECORD = '\u001e'

    fun encode(collections: List<UserCollection>): String =
        collections.joinToString("\n") { collection ->
            val title = collection.title.replace("\n", " ").replace("$UNIT", " ")
            val ids = collection.itemIds.joinToString("$RECORD") { id ->
                id.replace("$RECORD", "")
            }
            "${collection.id}$UNIT$title$UNIT$ids"
        }

    fun decode(text: String): List<UserCollection> {
        if (text.isBlank()) return emptyList()
        return text.lineSequence().mapNotNull { line ->
            val parts = line.split(UNIT)
            if (parts.size < 2) return@mapNotNull null
            val id = parts[0].trim()
            val title = parts[1].trim()
            if (id.isEmpty() || title.isEmpty()) return@mapNotNull null
            val ids = parts.getOrNull(2).orEmpty()
                .split(RECORD)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            UserCollection(id = id, title = title, itemIds = ids)
        }.toList()
    }
}
