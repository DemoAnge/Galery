package com.example.galeria.features.gallery.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.VideoPlayer
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GalleryViewerScreen(
    session: ViewerSession,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onMoveToVault: () -> Unit,
    onAddToAlbum: () -> Unit,
) {
    if (session.items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val pagerState = rememberPagerState(
        initialPage = session.index.coerceIn(0, session.items.lastIndex),
        pageCount = { session.items.size.coerceAtLeast(1) },
    )
    LaunchedEffect(pagerState.settledPage) {
        onPageChanged(pagerState.settledPage)
    }
    LaunchedEffect(session.item.id, session.index) {
        val target = session.index.coerceIn(0, (session.items.size - 1).coerceAtLeast(0))
        if (pagerState.pageCount > 0 && pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }
    val item = session.items.getOrElse(pagerState.currentPage) { session.item }
    val isFavorite = item.id in favoriteIds
    var chromeVisible by remember { mutableStateOf(true) }
    var confirmMove by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val dismiss = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val scrimAlpha = (1f - (abs(dismiss.value) / 600f)).coerceIn(0.25f, 1f)
    LaunchedEffect(chromeVisible, item.id, confirmMove, confirmDelete) {
        if (!chromeVisible || confirmMove || confirmDelete) return@LaunchedEffect
        delay(3_000)
        chromeVisible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha)),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = abs(dismiss.value) < 8f,
            beyondViewportPageCount = 0,
        ) { page ->
            val pageItem = session.items.getOrNull(page) ?: session.items.first()
            val active = page == pagerState.settledPage && abs(dismiss.value) < 8f
            ZoomablePage(
                item = pageItem,
                image = if (page == session.index) session.image else null,
                active = active,
                chromeVisible = chromeVisible,
                enabled = abs(dismiss.value) < 8f,
                onTap = { chromeVisible = !chromeVisible },
                onDoubleTapShowChrome = { chromeVisible = true },
                onVerticalDrag = { dy ->
                    if (!chromeVisible && dy < 0f) {
                        chromeVisible = true
                        scope.launch { dismiss.snapTo(0f) }
                    } else if (dy > 0f || dismiss.value > 0f) {
                        scope.launch { dismiss.snapTo((dismiss.value + dy).coerceAtLeast(0f)) }
                    }
                },
                onVerticalDragEnd = {
                    scope.launch {
                        if (dismiss.value > 140f) {
                            onBack()
                        } else {
                            dismiss.animateTo(0f, tween(180))
                        }
                    }
                },
                modifier = Modifier.graphicsLayer { translationY = dismiss.value },
            )
        }
        if (session.busy) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }
        if (chromeVisible) {
            ViewerTopBar(
                onBack = onBack,
                onShare = onShare,
                onEdit = onEdit,
                canEdit = item.mediaKind == GalleryMediaKind.IMAGE ||
                    item.mediaKind == GalleryMediaKind.VIDEO,
                item = item,
                onAddToAlbum = onAddToAlbum,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .statusBarsPadding(),
            )
            ViewerBottomBar(
                isFavorite = isFavorite,
                canEdit = item.mediaKind == GalleryMediaKind.IMAGE ||
                    item.mediaKind == GalleryMediaKind.VIDEO,
                busy = session.busy,
                error = session.error,
                onShare = onShare,
                onEdit = onEdit,
                onFavorite = onFavorite,
                onDelete = { confirmDelete = true },
                onMoveToVault = { confirmMove = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .navigationBarsPadding(),
            )
        }
        if (confirmMove) {
            MoveToVaultConfirmDialog(
                count = 1,
                onConfirm = {
                    confirmMove = false
                    onMoveToVault()
                },
                onDismiss = { confirmMove = false },
            )
        }
        if (confirmDelete) {
            GalleryDeleteConfirmDialog(
                count = 1,
                onConfirm = {
                    confirmDelete = false
                    onDelete()
                },
                onDismiss = { confirmDelete = false },
            )
        }
    }
}

@Composable
private fun ZoomablePage(
    item: MediaItem,
    image: androidx.compose.ui.graphics.ImageBitmap?,
    active: Boolean,
    chromeVisible: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onDoubleTapShowChrome: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onVerticalDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVideo = item.mediaKind == GalleryMediaKind.VIDEO
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    val chromeVisibleState = rememberUpdatedState(chromeVisible)
    val enabledState = rememberUpdatedState(enabled)
    val onTapState = rememberUpdatedState(onTap)
    val onDoubleTapShowChromeState = rememberUpdatedState(onDoubleTapShowChrome)
    val onVerticalDragState = rememberUpdatedState(onVerticalDrag)
    val onVerticalDragEndState = rememberUpdatedState(onVerticalDragEnd)
    val transformableState = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale > 1.02f) offset + pan else Offset.Zero
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (isVideo) Modifier else Modifier.transformable(state = transformableState, enabled = enabled),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            image != null && item.mediaKind == GalleryMediaKind.IMAGE -> {
                Image(
                    bitmap = image,
                    contentDescription = item.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentScale = ContentScale.Fit,
                )
            }
            item.mediaKind == GalleryMediaKind.VIDEO -> {
                VideoPage(
                    item = item,
                    poster = image,
                    active = active,
                )
            }
            else -> {
                MediaThumb(item = item, modifier = Modifier.fillMaxSize())
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(item.id) {
                    detectTapGestures(
                        onTap = { onTapState.value() },
                        onDoubleTap = {
                            if (!chromeVisibleState.value) {
                                onDoubleTapShowChromeState.value()
                            } else if (!isVideo) {
                                if (scale > 1.1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.4f
                                }
                            } else {
                                onTapState.value()
                            }
                        },
                    )
                }
                .pointerInput(item.id) {
                    detectVerticalDragGestures(
                        onDragEnd = { onVerticalDragEndState.value() },
                        onDragCancel = { onVerticalDragEndState.value() },
                        onVerticalDrag = { _, dragAmount ->
                            if (!enabledState.value || scale > 1.05f) return@detectVerticalDragGestures
                            onVerticalDragState.value(dragAmount)
                        },
                    )
                },
        )
    }
}

@Composable
private fun VideoPage(
    item: MediaItem,
    poster: androidx.compose.ui.graphics.ImageBitmap?,
    active: Boolean,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (active) {
            VideoPlayer(
                uri = item.publicUri,
                playWhenReady = true,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            if (poster != null) {
                Image(
                    bitmap = poster,
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                MediaThumb(item = item, modifier = Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    GaleriaIcons.Play,
                    contentDescription = "Reproducir",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            if (item.durationMs > 0L) {
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ViewerTopBar(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    canEdit: Boolean,
    item: MediaItem,
    onAddToAlbum: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var more by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(GaleriaIcons.Back, contentDescription = "Cerrar", tint = Color.White)
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onShare) {
            Icon(GaleriaIcons.Share, contentDescription = "Compartir", tint = Color.White)
        }
        if (canEdit) {
            IconButton(onClick = onEdit) {
                Icon(GaleriaIcons.Edit, contentDescription = "Editar", tint = Color.White)
            }
        }
        Box {
            IconButton(onClick = { more = true }) {
                Icon(GaleriaIcons.More, contentDescription = "Más", tint = Color.White)
            }
            DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                DropdownMenuItem(
                    text = { Text(item.displayName) },
                    onClick = { more = false },
                )
                DropdownMenuItem(
                    text = { Text(daySectionFor(item.capturedAtEpochMs).label) },
                    onClick = { more = false },
                )
                DropdownMenuItem(
                    text = { Text("Mover al álbum") },
                    onClick = {
                        more = false
                        onAddToAlbum()
                    },
                )
            }
        }
    }
}

@Composable
private fun ViewerBottomBar(
    isFavorite: Boolean,
    canEdit: Boolean,
    busy: Boolean,
    error: String?,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
    onMoveToVault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ViewerAction(
                icon = GaleriaIcons.Share,
                label = "Compartir",
                enabled = !busy,
                onClick = onShare,
            )
            if (canEdit) {
                ViewerAction(
                    icon = GaleriaIcons.Edit,
                    label = "Editar",
                    enabled = !busy,
                    onClick = onEdit,
                )
            }
            ViewerAction(
                icon = if (isFavorite) GaleriaIcons.FavoriteFilled else GaleriaIcons.FavoriteOutline,
                label = "Favorito",
                enabled = !busy,
                onClick = onFavorite,
            )
            ViewerAction(
                icon = GaleriaIcons.Delete,
                label = "Eliminar",
                enabled = !busy,
                onClick = onDelete,
            )
            ViewerAction(
                icon = GaleriaIcons.VaultOutline,
                label = "Vault",
                enabled = !busy,
                onClick = onMoveToVault,
            )
        }
    }
}

@Composable
private fun ViewerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(56.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}
