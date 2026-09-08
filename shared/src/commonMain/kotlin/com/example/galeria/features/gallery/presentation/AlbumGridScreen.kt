package com.example.galeria.features.gallery.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.Album
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.ui.GaleriaIcons

@Composable
fun AlbumGridScreen(
    album: Album,
    selectedIds: Set<String>,
    selecting: Boolean,
    message: String?,
    onBack: () -> Unit,
    onOpenItem: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    onAddPhotos: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(GaleriaIcons.Back, contentDescription = "Volver a álbumes")
            }
            Column(Modifier.weight(1f)) {
                Text(album.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${album.items.size} ${if (album.items.size == 1) "elemento" else "elementos"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onAddPhotos != null) {
                IconButton(onClick = onAddPhotos) {
                    Icon(GaleriaIcons.Plus, contentDescription = "Mover fotos a este álbum")
                }
            }
        }
        if (message != null) {
            Text(
                message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (album.items.isEmpty() && onAddPhotos != null) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(onClick = onAddPhotos),
            ) {
                EmptyPhotosHint(
                    title = "Álbum vacío",
                    body = "Pulsa + para mover fotos o videos a esta colección.",
                )
            }
        } else {
            PhotosMasonry(
                items = album.items,
                selectedIds = selectedIds,
                selecting = selecting,
                onOpen = { item, _ -> onOpenItem(item) },
                onLongPress = onLongPress,
                onToggleSelect = onToggleSelect,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
