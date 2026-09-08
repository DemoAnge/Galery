package com.example.galeria.features.gallery.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.Album
import com.example.galeria.features.gallery.domain.AlbumId
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.ui.AlbumCoverRadius
import com.example.galeria.ui.GaleriaIcons

@Composable
fun CollectionsScreen(
    albums: List<Album>,
    allItems: List<MediaItem>,
    favorites: Set<String>,
    message: String?,
    onOpenAlbum: (Album) -> Unit,
    onCreateAlbum: () -> Unit,
    onOpenVault: () -> Unit,
) {
    val libraryAlbums = buildLibraryAlbums(albums, allItems, favorites)
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Colecciones",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCreateAlbum) {
                Icon(GaleriaIcons.Plus, contentDescription = "Crear o mover a un álbum")
            }
            IconButton(onClick = onOpenVault) {
                Icon(GaleriaIcons.VaultOutline, contentDescription = "Vault")
            }
        }
        if (message != null) {
            Text(
                message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(libraryAlbums, key = { it.customId ?: "${it.id.name}:${it.title}" }) { album ->
                AlbumCard(album = album, onClick = { onOpenAlbum(album) })
            }
        }
    }
}

internal const val VIDEOS_COLLECTION_TITLE = "Videos"

internal fun buildLibraryAlbums(
    albums: List<Album>,
    allItems: List<MediaItem>,
    favorites: Set<String>,
): List<Album> {
    val favItems = allItems.filter { it.id in favorites }
    val fav = if (favItems.isEmpty()) {
        emptyList()
    } else {
        listOf(Album(id = AlbumId.OTHER, title = "Favoritos", items = favItems))
    }
    val videos = allItems.filter { it.mediaKind == GalleryMediaKind.VIDEO }
    val videoAlbum = if (videos.isEmpty()) {
        emptyList()
    } else {
        listOf(Album(id = AlbumId.OTHER, title = VIDEOS_COLLECTION_TITLE, items = videos))
    }
    return fav + videoAlbum + albums
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    val cover = album.items.firstOrNull()
    val shape = RoundedCornerShape(AlbumCoverRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${album.title}, ${album.items.size} elementos" },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(8.dp, shape)
                .clip(shape),
        ) {
            MediaThumb(item = cover, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                        ),
                    )
                    .padding(10.dp),
            )
            if (cover?.mediaKind == GalleryMediaKind.VIDEO) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            GaleriaIcons.Play,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            if (album.title == VIDEOS_COLLECTION_TITLE) {
                                "${album.items.size}"
                            } else {
                                formatDuration(cover.durationMs)
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
        Text(
            album.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "${album.items.size} ${if (album.items.size == 1) "elemento" else "elementos"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
