package com.example.galeria.features.gallery.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.PhotoGridGap
import com.example.galeria.ui.TileRadius

@Composable
internal fun PhotosMasonry(
    items: List<MediaItem>,
    selectedIds: Set<String>,
    selecting: Boolean,
    onOpen: (MediaItem, List<MediaItem>) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    DatedPhotoGrid(
        items = items,
        selectedIds = selectedIds,
        selecting = selecting,
        onOpen = onOpen,
        onLongPress = onLongPress,
        onToggleSelect = onToggleSelect,
        modifier = modifier,
    )
}

@Composable
internal fun PhotosFlatGrid(
    items: List<MediaItem>,
    selectedIds: Set<String>,
    selecting: Boolean,
    onOpen: (MediaItem) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    DatedPhotoGrid(
        items = items,
        selectedIds = selectedIds,
        selecting = selecting,
        onOpen = { item, _ -> onOpen(item) },
        onLongPress = onLongPress,
        onToggleSelect = onToggleSelect,
        modifier = modifier,
    )
}

@Composable
internal fun DatedPhotoGrid(
    items: List<MediaItem>,
    selectedIds: Set<String>,
    selecting: Boolean,
    onOpen: (MediaItem, List<MediaItem>) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    emptyTitle: String = "Sin fotos todavía",
    emptyBody: String = "Concede acceso a todas las fotos (no solo una selección) para ver Cámara, WhatsApp y descargas.",
    header: (@Composable () -> Unit)? = null,
    sections: List<TimelineGroup>? = null,
) {
    val groups = sections ?: remember(items) { groupByDay(items) }
    val stacked = remember(groups) { groups.map { group -> group to stackNearby(group.items) } }
    if (stacked.isEmpty()) {
        EmptyPhotosHint(title = emptyTitle, body = emptyBody)
        return
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(110.dp),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(PhotoGridGap),
        verticalItemSpacing = PhotoGridGap,
        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, bottom = 24.dp),
    ) {
        if (header != null) {
            item(key = "feed-header", span = StaggeredGridItemSpan.FullLine) {
                header()
            }
        }
        stacked.forEach { (group, stacks) ->
            item(
                key = "h-${group.key}",
                span = StaggeredGridItemSpan.FullLine,
            ) {
                DateStickyHeader(group.label)
            }
            stacks.forEachIndexed { index, stack ->
                val role = stack.cover.feedCardRole(index, stacks.size)
                item(
                    key = "${group.key}-${stack.cover.id}",
                    span = StaggeredGridItemSpan.SingleLane,
                ) {
                    PhotoTile(
                        item = stack.cover,
                        selected = stack.items.any { it.id in selectedIds },
                        selecting = selecting,
                        stackCount = stack.items.size,
                        aspectRatio = stack.cover.cardAspectRatio(role),
                        thumbEdge = if (role == FeedCardRole.TILE) 256 else 384,
                        onOpen = { onOpen(stack.cover, stack.items) },
                        onLongPress = { onLongPress(stack.cover) },
                        onToggleSelect = { onToggleSelect(stack.cover) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoTile(
    item: MediaItem,
    selected: Boolean,
    selecting: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelect: () -> Unit,
    modifier: Modifier = Modifier,
    stackCount: Int = 1,
    aspectRatio: Float = item.tileAspectRatio(),
    thumbEdge: Int = 256,
) {
    val shape = RoundedCornerShape(TileRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(shape)
            .combinedClickable(
                onClick = { if (selecting) onToggleSelect() else onOpen() },
                onLongClick = { if (selecting) onToggleSelect() else onLongPress() },
            )
            .semantics { contentDescription = item.displayName },
    ) {
        MediaThumb(item = item, modifier = Modifier.fillMaxSize(), maxEdge = thumbEdge)
        if (item.mediaKind == GalleryMediaKind.VIDEO) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.62f),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = GaleriaIcons.Play,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = formatDuration(item.durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
        }
        if (stackCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(
                    GaleriaIcons.Stack,
                    contentDescription = "Varias fotos",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    "+${stackCount - 1}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (selecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (selected) 0.28f else 0.08f)),
            )
            val scheme = MaterialTheme.colorScheme
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) scheme.primary else Color.White.copy(alpha = 0.88f))
                    .border(
                        width = 1.dp,
                        color = if (selected) scheme.primary else Color.White.copy(alpha = 0.9f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = GaleriaIcons.Check,
                        contentDescription = "Seleccionado",
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun DateStickyHeader(label: String) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
        )
    }
}

@Composable
internal fun EmptyPhotosHint(
    title: String = "Sin fotos todavía",
    body: String = "Concede acceso a todas las fotos (no solo una selección) para ver Cámara, WhatsApp y descargas.",
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
