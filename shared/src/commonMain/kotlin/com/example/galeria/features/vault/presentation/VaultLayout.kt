package com.example.galeria.features.vault.presentation

import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultMediaKind

data class VaultCollection(
    val title: String,
    val items: List<VaultItem>,
)

fun vaultPhotos(items: List<VaultItem>): List<VaultItem> =
    items.filter { it.mediaKind == VaultMediaKind.IMAGE }

fun vaultVideos(items: List<VaultItem>): List<VaultItem> =
    items.filter { it.mediaKind == VaultMediaKind.VIDEO }

fun vaultCollections(items: List<VaultItem>): List<VaultCollection> {
    val groups = items
        .mapNotNull { item ->
            val label = item.albumLabel?.trim().orEmpty()
            if (label.isEmpty()) null else label to item
        }
        .groupBy({ it.first }, { it.second })
        .map { (title, grouped) ->
            VaultCollection(
                title = title,
                items = grouped.sortedByDescending { it.capturedAtEpochMs },
            )
        }
        .sortedByDescending { it.items.size }
    val grouped = groups.size >= 2 || groups.any { it.items.size >= 2 }
    return if (grouped) groups else emptyList()
}

fun vaultCollectionByTitle(items: List<VaultItem>, title: String?): VaultCollection? {
    if (title.isNullOrBlank()) return null
    return vaultCollections(items).firstOrNull { it.title == title }
}
