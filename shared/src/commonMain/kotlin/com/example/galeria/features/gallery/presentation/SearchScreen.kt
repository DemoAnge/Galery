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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.AlbumId
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.features.gallery.domain.title
import com.example.galeria.ui.GaleriaIcons

enum class SearchChip(val label: String) {
    VIDEOS("Videos"),
    SCREENSHOTS("Capturas"),
    CAMERA("Cámara"),
    WHATSAPP("WhatsApp"),
    DOWNLOADS("Descargas"),
    FAVORITES("Favoritos"),
}

@Composable
fun SearchScreen(
    items: List<MediaItem>,
    favorites: Set<String>,
    selectedIds: Set<String>,
    selecting: Boolean,
    onOpen: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    onClose: (() -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    var chip by remember { mutableStateOf<SearchChip?>(null) }
    val results = remember(items, favorites, query, chip) {
        filterSearch(items, favorites, query, chip)
    }
    Column(Modifier.fillMaxSize()) {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Buscar en fotos") },
            leadingIcon = {
                Icon(GaleriaIcons.SearchOutline, contentDescription = null)
            },
            trailingIcon = if (onClose != null) {
                {
                    IconButton(onClick = onClose) {
                        Icon(GaleriaIcons.Close, contentDescription = "Cerrar búsqueda")
                    }
                }
            } else {
                null
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchChip.entries.forEach { option ->
                FilterChip(
                    selected = chip == option,
                    onClick = { chip = if (chip == option) null else option },
                    label = { Text(option.label) },
                    shape = RoundedCornerShape(50),
                )
            }
        }
        if (query.isBlank() && chip == null) {
            Text(
                "Prueba un álbum, un tipo de archivo o un nombre.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        } else {
            PhotosFlatGrid(
                items = results,
                selectedIds = selectedIds,
                selecting = selecting,
                onOpen = onOpen,
                onLongPress = onLongPress,
                onToggleSelect = onToggleSelect,
            )
        }
    }
}

private fun filterSearch(
    items: List<MediaItem>,
    favorites: Set<String>,
    query: String,
    chip: SearchChip?,
): List<MediaItem> {
    var result = items
    result = when (chip) {
        SearchChip.VIDEOS -> result.filter { it.mediaKind == GalleryMediaKind.VIDEO }
        SearchChip.SCREENSHOTS -> result.filter { it.albumId == AlbumId.SCREENSHOTS }
        SearchChip.CAMERA -> result.filter { it.albumId == AlbumId.CAMERA }
        SearchChip.WHATSAPP -> result.filter { it.albumId == AlbumId.WHATSAPP }
        SearchChip.DOWNLOADS -> result.filter { it.albumId == AlbumId.DOWNLOADS }
        SearchChip.FAVORITES -> result.filter { it.id in favorites }
        null -> result
    }
    val q = query.trim().lowercase()
    if (q.isNotEmpty()) {
        result = result.filter { item ->
            item.displayName.lowercase().contains(q) ||
                item.bucketName.orEmpty().lowercase().contains(q) ||
                item.albumId.title().lowercase().contains(q)
        }
    }
    return result
}
