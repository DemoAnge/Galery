package com.example.galeria.features.vault.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultMediaKind
import com.example.galeria.ui.AlbumCoverRadius
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.PhotoGridGap
import com.example.galeria.ui.TileRadius

@Composable
fun VaultGridScreen(
    state: VaultUiState.Unlocked,
    onOpenItem: (VaultItem, List<VaultItem>) -> Unit,
    onLeave: () -> Unit,
    onOpenCollection: (String) -> Unit,
    onCloseCollection: () -> Unit,
    onEnableBiometric: () -> Unit,
    onSkipBiometric: () -> Unit,
    loadThumbnail: suspend (VaultItem) -> ImageBitmap?,
) {
    val photos = remember(state.items) { vaultPhotos(state.items) }
    val videos = remember(state.items) { vaultVideos(state.items) }
    val collections = remember(state.items) { vaultCollections(state.items) }
    val opened = remember(state.items, state.openedCollection) {
        vaultCollectionByTitle(state.items, state.openedCollection)
    }

    Column(Modifier.fillMaxSize()) {
        VaultHeader(
            title = opened?.title ?: "Vault",
            subtitle = when {
                opened != null -> "${opened.items.size} ${if (opened.items.size == 1) "elemento" else "elementos"}"
                state.items.isEmpty() -> "Vacío"
                else -> "${state.items.size} ${if (state.items.size == 1) "privado" else "privados"}"
            },
            onBack = if (opened != null) onCloseCollection else onLeave,
        )
        if (state.message != null) {
            Text(
                state.message,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (state.offerBiometric && opened == null) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "¿Activar huella o Face ID como atajo? Seguirás necesitando PIN o patrón.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Button(onClick = onEnableBiometric) { Text("Activar") }
                    OutlinedButton(onClick = onSkipBiometric) { Text("Ahora no") }
                }
            }
        }
        when {
            opened != null -> VaultMediaGrid(
                items = opened.items,
                onOpenItem = { item -> onOpenItem(item, opened.items) },
                loadThumbnail = loadThumbnail,
            )
            state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Vault vacío.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            else -> VaultHomeGrid(
                photos = photos,
                videos = videos,
                collections = collections,
                onOpenItem = onOpenItem,
                onOpenCollection = onOpenCollection,
                loadThumbnail = loadThumbnail,
            )
        }
    }
}

@Composable
private fun VaultHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(GaleriaIcons.Back, contentDescription = "Salir")
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VaultHomeGrid(
    photos: List<VaultItem>,
    videos: List<VaultItem>,
    collections: List<VaultCollection>,
    onOpenItem: (VaultItem, List<VaultItem>) -> Unit,
    onOpenCollection: (String) -> Unit,
    loadThumbnail: suspend (VaultItem) -> ImageBitmap?,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(PhotoGridGap),
        verticalArrangement = Arrangement.spacedBy(PhotoGridGap),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 20.dp),
    ) {
        if (photos.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "h-photos") {
                VaultSectionTitle("Fotos", photos.size)
            }
            items(photos, key = { "p-${it.id.value}" }) { item ->
                VaultGridCell(
                    item = item,
                    onClick = { onOpenItem(item, photos) },
                    loadThumbnail = loadThumbnail,
                )
            }
        }
        if (videos.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "h-videos") {
                VaultSectionTitle("Videos", videos.size)
            }
            items(videos, key = { "v-${it.id.value}" }) { item ->
                VaultGridCell(
                    item = item,
                    onClick = { onOpenItem(item, videos) },
                    loadThumbnail = loadThumbnail,
                )
            }
        }
        if (collections.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "h-collections") {
                VaultSectionTitle("Colecciones", collections.size)
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "collections-panel") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    collections.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { collection ->
                                Box(Modifier.weight(1f)) {
                                    VaultCollectionCard(
                                        collection = collection,
                                        onClick = { onOpenCollection(collection.title) },
                                        loadThumbnail = loadThumbnail,
                                    )
                                }
                            }
                            if (row.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultMediaGrid(
    items: List<VaultItem>,
    onOpenItem: (VaultItem) -> Unit,
    loadThumbnail: suspend (VaultItem) -> ImageBitmap?,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(PhotoGridGap),
        verticalArrangement = Arrangement.spacedBy(PhotoGridGap),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 20.dp),
    ) {
        items(items, key = { it.id.value }) { item ->
            VaultGridCell(
                item = item,
                onClick = { onOpenItem(item) },
                loadThumbnail = loadThumbnail,
            )
        }
    }
}

@Composable
private fun VaultSectionTitle(title: String, count: Int) {
    Text(
        "$title · $count",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 8.dp),
    )
}

@Composable
private fun VaultGridCell(
    item: VaultItem,
    onClick: () -> Unit,
    loadThumbnail: suspend (VaultItem) -> ImageBitmap?,
) {
    var image by remember(item.id.value) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item.id.value) {
        image = loadThumbnail(item)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(TileRadius))
            .clickable(onClick = onClick),
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (item.mediaKind == VaultMediaKind.IMAGE) "Foto" else "Video",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (item.mediaKind == VaultMediaKind.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    GaleriaIcons.Play,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun VaultCollectionCard(
    collection: VaultCollection,
    onClick: () -> Unit,
    loadThumbnail: suspend (VaultItem) -> ImageBitmap?,
) {
    val cover = collection.items.firstOrNull()
    var image by remember(cover?.id?.value) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(cover?.id?.value) {
        image = cover?.let { loadThumbnail(it) }
    }
    val shape = RoundedCornerShape(AlbumCoverRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape),
        ) {
            val bitmap = image
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = collection.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        GaleriaIcons.CollectionsOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        ),
                    )
                    .padding(8.dp),
            ) {
                Text(
                    "${collection.items.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Text(
            collection.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
