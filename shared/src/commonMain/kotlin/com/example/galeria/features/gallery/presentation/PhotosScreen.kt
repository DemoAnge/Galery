package com.example.galeria.features.gallery.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.Album
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.ui.AccentsLavender
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.GlassScrim
import com.example.galeria.ui.MemoryCardHeight
import com.example.galeria.ui.MemoryCardRadius
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PhotosScreen(
    items: List<MediaItem>,
    albums: List<Album>,
    favorites: Set<String>,
    selectedIds: Set<String>,
    selecting: Boolean,
    message: String?,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (MediaItem, List<MediaItem>) -> Unit,
    onOpenMemory: (MemoryCard) -> Unit,
    onLongPress: (MediaItem) -> Unit,
    onToggleSelect: (MediaItem) -> Unit,
    onCreateAlbum: () -> Unit,
    onOpenVault: () -> Unit,
) {
    val memories = remember(items, albums, favorites) { memoryCards(items, albums, favorites) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize()) {
            PhotosHeader(
                count = items.size,
                onCreateAlbum = onCreateAlbum,
                onOpenVault = onOpenVault,
            )
            if (message != null) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            DatedPhotoGrid(
                items = items,
                selectedIds = selectedIds,
                selecting = selecting,
                onOpen = { item, _ -> onOpen(item, items) },
                onLongPress = onLongPress,
                onToggleSelect = onToggleSelect,
                modifier = Modifier.weight(1f),
                header = if (!selecting && memories.isNotEmpty()) {
                    { MemoryCarousel(cards = memories, onOpen = onOpenMemory) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun PhotosHeader(
    count: Int,
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
                "Fotos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                if (count == 1) "1 elemento en el dispositivo" else "$count elementos en el dispositivo",
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
private fun MemoryCarousel(
    cards: List<MemoryCard>,
    onOpen: (MemoryCard) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { cards.size })
    val endPeek = if (cards.size > 1) 56.dp else 16.dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(PreferHorizontalScroll)
            .padding(top = 6.dp, bottom = 4.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(MemoryCardHeight),
            contentPadding = PaddingValues(start = 16.dp, end = endPeek),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            userScrollEnabled = cards.size > 1,
            key = { index -> cards[index].id },
        ) { page ->
            val card = cards[page]
            MemoryCardView(
                card = card,
                live = page == pagerState.settledPage,
                onClick = { onOpen(card) },
            )
        }
        if (cards.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                cards.forEachIndexed { index, _ ->
                    val selected = index == pagerState.currentPage
                    val width by animateDpAsState(if (selected) 16.dp else 6.dp, label = "dump-dot")
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (selected) AccentsLavender else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                            ),
                    )
                }
            }
        }
    }
}

private val PreferHorizontalScroll = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        return if (abs(available.x) > 0f) Offset(available.x, 0f) else Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return if (abs(available.x) > 0f) available.copy(y = 0f) else Velocity.Zero
    }
}

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    live: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(MemoryCardRadius)
    val frames = remember(card.id, card.items) { card.items.take(4) }
    var frame by remember(card.id) { mutableIntStateOf(0) }
    LaunchedEffect(card.id, live, frames.size) {
        if (!live || frames.size <= 1) return@LaunchedEffect
        while (isActive) {
            delay(5_000)
            frame = (frame + 1) % frames.size
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = AccentsLavender.copy(alpha = 0.38f),
                spotColor = AccentsLavender.copy(alpha = 0.28f),
            )
            .clip(shape)
            .clickable(onClick = onClick),
    ) {
        val shown = frames.getOrNull(frame.coerceIn(0, (frames.size - 1).coerceAtLeast(0)))
        AnimatedContent(
            targetState = shown?.id,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(500)) },
            label = "dump-frame",
        ) { id ->
            val item = frames.firstOrNull { it.id == id } ?: shown
            Box(Modifier.fillMaxSize().clip(shape)) {
                MediaThumb(
                    item = item,
                    modifier = Modifier.fillMaxSize(),
                    maxEdge = 512,
                )
            }
        }
        if (frames.size >= 3 && shown != null) {
            MemoryCollagePeek(items = frames.take(3))
        }
        GlassScrim(modifier = Modifier.fillMaxSize(), shape = shape)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Text(
                "Recuerdos",
                color = AccentsLavender,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                card.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                card.subtitle,
                color = Color.White.copy(alpha = 0.86f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun BoxScope.MemoryCollagePeek(items: List<MediaItem>) {
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.drop(1).take(2).forEach { item ->
            MediaThumb(
                item = item,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        }
    }
}
