package com.example.galeria.features.gallery.domain

import kotlin.random.Random

data class UserCollection(
    val id: String,
    val title: String,
    val itemIds: List<String>,
)

fun newUserCollection(
    existing: List<UserCollection>,
    title: String,
    itemIds: List<String>,
): UserCollection = UserCollection(
    id = "c-${existing.size}-${title.hashCode()}-${Random.nextLong()}",
    title = title.trim(),
    itemIds = itemIds.distinct(),
)

fun List<UserCollection>.movingTo(
    collectionId: String,
    itemIds: List<String>,
    sourceCustomId: String? = null,
): List<UserCollection> {
    val ids = itemIds.distinct()
    if (ids.isEmpty()) return this
    val removeFromSource = sourceCustomId != null && sourceCustomId != collectionId
    return map { collection ->
        when {
            collection.id == collectionId ->
                collection.copy(itemIds = (collection.itemIds + ids).distinct())
            removeFromSource && collection.id == sourceCustomId ->
                collection.copy(itemIds = collection.itemIds.filterNot { it in ids })
            else -> collection
        }
    }
}

fun List<UserCollection>.renaming(collectionId: String, title: String): List<UserCollection> {
    val name = title.trim()
    if (name.isEmpty()) return this
    return map { collection ->
        if (collection.id == collectionId) collection.copy(title = name) else collection
    }
}

fun List<UserCollection>.without(collectionId: String): List<UserCollection> =
    filterNot { it.id == collectionId }
