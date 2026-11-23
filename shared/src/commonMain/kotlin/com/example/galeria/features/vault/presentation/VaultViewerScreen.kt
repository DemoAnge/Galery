package com.example.galeria.features.vault.presentation

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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultMediaKind
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.VideoPlayer

@Composable
fun VaultViewerScreen(
    state: VaultUiState.Viewing,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val initial = state.items.indexOfFirst { it.id == state.item.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initial, pageCount = { state.items.size.coerceAtLeast(1) })
    var chromeVisible by remember { mutableStateOf(true) }
    var confirmDelete by remember { mutableStateOf(false) }
    val chromeVisibleState = rememberUpdatedState(chromeVisible)
    LaunchedEffect(pagerState.settledPage) {
        onPageChanged(pagerState.settledPage)
    }
    LaunchedEffect(chromeVisible, state.item.id) {
        if (!chromeVisible) return@LaunchedEffect
        delay(3_000)
        chromeVisible = false
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
        ) { page ->
            val pageItem = state.items.getOrNull(page) ?: state.item
            val active = page == pagerState.settledPage
            ZoomableVaultPage(
                item = pageItem,
                image = if (pageItem.id == state.item.id) state.image else null,
                playbackUri = if (pageItem.id == state.item.id) state.playbackUri else null,
                active = active,
                chromeVisible = chromeVisible,
                onTap = { chromeVisible = !chromeVisibleState.value },
                onDoubleTapShowChrome = { chromeVisible = true },
            )
        }
        if (state.busy) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
        if (chromeVisible) Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = !state.busy) {
                Icon(GaleriaIcons.Back, contentDescription = "Cerrar", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text(
                state.item.displayName,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(end = 8.dp),
            )
            if (state.item.mediaKind == VaultMediaKind.IMAGE) {
                IconButton(onClick = onEdit, enabled = !state.busy) {
                    Icon(GaleriaIcons.Edit, contentDescription = "Editar", tint = Color.White)
                }
            }
        }
        if (chromeVisible) Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextButton(onClick = onExport, enabled = !state.busy) {
                    Text(
                        if (state.busy) "Exportando…" else "Exportar",
                        color = Color.White,
                    )
                }
                TextButton(onClick = { confirmDelete = true }, enabled = !state.busy) {
                    Text("Borrar", color = Color.White)
                }
            }
        }
        if (confirmDelete) {
            VaultDeleteConfirmDialog(
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
private fun ZoomableVaultPage(
    item: VaultItem,
    image: ImageBitmap?,
    playbackUri: String?,
    active: Boolean,
    chromeVisible: Boolean,
    onTap: () -> Unit,
    onDoubleTapShowChrome: () -> Unit,
) {
    val isVideo = item.mediaKind == VaultMediaKind.VIDEO
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    val chromeVisibleState = rememberUpdatedState(chromeVisible)
    val onTapState = rememberUpdatedState(onTap)
    val onDoubleTapShowChromeState = rememberUpdatedState(onDoubleTapShowChrome)
    val transformableState = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale > 1.02f) offset + pan else Offset.Zero
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isVideo) Modifier else Modifier.transformable(state = transformableState),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isVideo -> {
                if (active && playbackUri != null) {
                    VideoPlayer(
                        uri = playbackUri,
                        playWhenReady = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = item.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Icon(
                        GaleriaIcons.Play,
                        contentDescription = "Video protegido",
                        tint = Color.White,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
            image != null -> Image(
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
            else -> Text(item.displayName, color = Color.White)
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
                    detectVerticalDragGestures { _, dragAmount ->
                        if (scale > 1.05f) return@detectVerticalDragGestures
                        if (!chromeVisibleState.value && dragAmount < 0f) {
                            onDoubleTapShowChromeState.value()
                        }
                    }
                },
        )
    }
}

expect fun decodeVaultImage(bytes: ByteArray): ImageBitmap?
