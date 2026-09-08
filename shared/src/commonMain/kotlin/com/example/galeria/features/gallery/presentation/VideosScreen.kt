package com.example.galeria.features.gallery.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.Album
import com.example.galeria.features.gallery.domain.AlbumId
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.ui.GaleriaIcons

internal const val ALL_VIDEOS_KEY = "all"

@Composable
fun VideosScreen(
    items: List<MediaItem>,
    albums: List<Album>,
    selectedIds: Set<String>,
    selecting: Boolean,
    message: String?,
    refreshing: Boolean,
    selectedAlbumKey: String,
    onSelectAlbum: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpen: (MediaItem, List<MediaItem>) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    onCreateAlbum: () -> Unit,
    onOpenVault: () -> Unit,
) {
    val videos = remember(items) { items.filter { it.mediaKind == GalleryMediaKind.VIDEO } }
    val classified = remember(albums, videos) { classifyVideoAlbums(albums, videos) }
    val selectedAlbum = classified.firstOrNull { videoAlbumKey(it) == selectedAlbumKey }
    val visible = selectedAlbum?.items ?: videos
    val sections = remember(selectedAlbum, classified) {
        if (selectedAlbum == null) videoSourceGroups(classified) else null
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize()) {
            VideosHeader(
                count = visible.size,
                sourceTitle = selectedAlbum?.title,
                onCreateAlbum = onCreateAlbum,
                onOpenVault = onOpenVault,
            )
            if (classified.size > 1) {
                VideoSourceFilter(
                    albums = classified,
                    selectedKey = if (selectedAlbum == null) ALL_VIDEOS_KEY else selectedAlbumKey,
                    onSelect = onSelectAlbum,
                )
            }
            if (message != null) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            DatedPhotoGrid(
                items = visible,
                selectedIds = selectedIds,
                selecting = selecting,
                onOpen = { item, _ -> onOpen(item, visible) },
                onLongPress = onLongPress,
                onToggleSelect = onToggleSelect,
                modifier = Modifier.weight(1f),
                emptyTitle = "Sin videos todavía",
                emptyBody = "Concede acceso a videos y espera a que se indexen en el dispositivo.",
                sections = sections,
            )
        }
    }
}

@Composable
private fun VideosHeader(
    count: Int,
    sourceTitle: String?,
    onCreateAlbum: () -> Unit,
    onOpenVault: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                sourceTitle ?: "Videos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                when {
                    sourceTitle != null && count == 1 -> "1 video en $sourceTitle"
                    sourceTitle != null -> "$count videos en $sourceTitle"
                    count == 1 -> "1 video en el dispositivo"
                    else -> "$count videos en el dispositivo"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onCreateAlbum) {
            Icon(GaleriaIcons.Plus, contentDescription = "Crear o mover a un álbum")
        }
        IconButton(onClick = onOpenVault) {
            Icon(GaleriaIcons.VaultOutline, contentDescription = "Vault")
        }
    }
}

@Composable
private fun VideoSourceFilter(
    albums: List<Album>,
    selectedKey: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedKey == ALL_VIDEOS_KEY,
            onClick = { onSelect(ALL_VIDEOS_KEY) },
            label = { Text("Todos") },
            shape = RoundedCornerShape(50),
        )
        albums.forEach { album ->
            val key = videoAlbumKey(album)
            FilterChip(
                selected = selectedKey == key,
                onClick = { onSelect(key) },
                label = { Text("${album.title} (${album.items.size})") },
                shape = RoundedCornerShape(50),
            )
        }
    }
}

internal fun videoAlbumKey(album: Album): String =
    "${album.customId.orEmpty()}:${album.id.name}:${album.title}"

internal fun classifyVideoAlbums(albums: List<Album>, allVideos: List<MediaItem>): List<Album> {
    val videos = allVideos.filter { it.mediaKind == GalleryMediaKind.VIDEO }
    val autoOrder = listOf(AlbumId.CAMERA, AlbumId.SCREENSHOTS, AlbumId.WHATSAPP, AlbumId.DOWNLOADS)
    val fromAlbums = albums
        .filter { it.customId == null }
        .mapNotNull { album ->
            val items = album.items.filter { it.mediaKind == GalleryMediaKind.VIDEO }
            if (items.isEmpty()) null else album.copy(items = items)
        }
    val auto = autoOrder.flatMap { id -> fromAlbums.filter { it.id == id } }
    val others = fromAlbums
        .filter { it.id == AlbumId.OTHER }
        .sortedBy { it.title.lowercase() }
    val classified = auto + others
    val seen = classified.flatMap { album -> album.items.map { it.id } }.toSet()
    val leftover = videos.filter { it.id !in seen }
    if (leftover.isEmpty()) return classified
    val otrosIndex = classified.indexOfFirst { it.id == AlbumId.OTHER && it.title == "Otros" }
    return if (otrosIndex >= 0) {
        classified.mapIndexed { index, album ->
            if (index != otrosIndex) {
                album
            } else {
                album.copy(
                    items = (album.items + leftover)
                        .distinctBy { it.id }
                        .sortedByDescending { it.capturedAtEpochMs },
                )
            }
        }
    } else {
        classified + Album(id = AlbumId.OTHER, title = "Otros", items = leftover)
    }
}

internal fun videoSourceGroups(albums: List<Album>): List<TimelineGroup> =
    albums.map { album ->
        TimelineGroup(
            key = videoAlbumKey(album),
            label = album.title,
            items = album.items,
        )
    }
