package com.example.galeria.features.gallery.presentation

import com.example.galeria.features.gallery.domain.Album
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.features.gallery.domain.videoFrameAspect
import kotlin.math.abs

data class MediaStack(
    val cover: MediaItem,
    val items: List<MediaItem>,
) {
    val extraCount: Int get() = (items.size - 1).coerceAtLeast(0)
}

data class MemoryCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val cover: MediaItem?,
    val items: List<MediaItem>,
)

fun MediaItem.tileAspectRatio(): Float {
    if (widthPx > 0 && heightPx > 0) {
        return videoFrameAspect(widthPx, heightPx)
    }
    if (mediaKind == GalleryMediaKind.VIDEO) return 9f / 16f
    val variants = floatArrayOf(0.56f, 0.68f, 0.82f, 1f, 1.18f, 1.33f, 1.52f, 0.74f, 1.68f)
    val index = abs(id.hashCode()) % variants.size
    return variants[index]
}

enum class FeedCardRole {
    TILE,
    GRANDE,
    SUPER,
}

fun FeedCardRole.isFullLine(): Boolean = false

fun MediaItem.feedCardRole(indexInGroup: Int, groupSize: Int): FeedCardRole {
    if (groupSize <= 1) return FeedCardRole.SUPER
    if (indexInGroup == 0) {
        return if (tileAspectRatio() >= 1f) FeedCardRole.SUPER else FeedCardRole.GRANDE
    }
    if (indexInGroup % 6 == 3) return FeedCardRole.SUPER
    if (indexInGroup % 3 == 2) return FeedCardRole.GRANDE
    return FeedCardRole.TILE
}

fun MediaItem.cardAspectRatio(role: FeedCardRole): Float {
    val ratio = tileAspectRatio()
    val scale = when (role) {
        FeedCardRole.TILE -> 1f
        FeedCardRole.GRANDE -> 0.92f
        FeedCardRole.SUPER -> 0.84f
    }
    return (ratio * scale).coerceIn(0.48f, 1.85f)
}

fun MediaItem.featuredAspectRatio(): Float = cardAspectRatio(FeedCardRole.SUPER)

fun MediaItem.shouldFeatureInFeed(indexInGroup: Int, groupSize: Int): Boolean =
    feedCardRole(indexInGroup, groupSize).isFullLine()

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0:00"
    val total = (durationMs / 1000L).toInt().coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    val sec = seconds.toString().padStart(2, '0')
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:$sec"
    } else {
        "$minutes:$sec"
    }
}

fun stackNearby(items: List<MediaItem>, windowMs: Long = 750L): List<MediaStack> {
    if (items.isEmpty()) return emptyList()
    val ordered = items.sortedByDescending { it.capturedAtEpochMs }
    val stacks = ArrayList<MediaStack>()
    var index = 0
    while (index < ordered.size) {
        val cluster = ArrayList<MediaItem>()
        val first = ordered[index]
        cluster += first
        var cursor = index + 1
        while (cursor < ordered.size && cluster.size < 8) {
            val next = ordered[cursor]
            val delta = abs(cluster.last().capturedAtEpochMs - next.capturedAtEpochMs)
            if (delta > windowMs) break
            cluster += next
            cursor++
        }
        // A dump import often shares DATE_ADDED; never hide a burst behind one tile.
        if (cluster.size >= 4) {
            ordered.subList(index, cursor).forEach { item ->
                stacks += MediaStack(cover = item, items = listOf(item))
            }
        } else {
            stacks += MediaStack(cover = first, items = cluster)
        }
        index = cursor
    }
    return stacks
}

fun memoryCards(items: List<MediaItem>, albums: List<Album>, favorites: Set<String>): List<MemoryCard> {
    if (items.isEmpty()) return emptyList()
    val cards = ArrayList<MemoryCard>()
    val seen = HashSet<String>()
    fun add(id: String, title: String, subtitle: String, list: List<MediaItem>) {
        if (list.isEmpty() || !seen.add(id)) return
        cards += MemoryCard(
            id = id,
            title = title,
            subtitle = subtitle,
            cover = list.first(),
            items = list,
        )
    }

    groupByDay(items)
        .filter { it.items.size >= 3 }
        .take(10)
        .forEach { group ->
            add("day-${group.key}", group.label, "${group.items.size} recuerdos", group.items)
        }

    val newest = items.maxOf { it.capturedAtEpochMs }
    val weekendWindow = 3L * 24L * 60L * 60L * 1000L
    val weekend = items.filter { newest - it.capturedAtEpochMs <= weekendWindow }
    if (weekend.size >= 2) {
        val photos = weekend.count { it.mediaKind == GalleryMediaKind.IMAGE }
        val videoCount = weekend.size - photos
        add("weekend", "Dump del fin de semana", "$photos fotos · $videoCount videos", weekend)
    }

    val videos = items.filter { it.mediaKind == GalleryMediaKind.VIDEO }
    add("videos", "Revive el momento", "${videos.size} videos", videos)

    val favItems = items.filter { it.id in favorites }
    add("favorites", "Favoritos", "${favItems.size} elementos", favItems)

    albums.filter { it.items.size >= 2 }
        .take(4)
        .forEach { album ->
            add("album-${album.id.name}-${album.title}", album.title, "${album.items.size} elementos", album.items)
        }
    return cards
}
