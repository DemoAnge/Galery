package com.example.galeria.features.vault.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.galeria.features.vault.domain.VaultMediaKind
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.VideoPlayer

@Composable
fun VaultViewerScreen(
    state: VaultUiState.Viewing,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val initial = state.items.indexOfFirst { it.id == state.item.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initial, pageCount = { state.items.size.coerceAtLeast(1) })
    LaunchedEffect(pagerState.settledPage) {
        onPageChanged(pagerState.settledPage)
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
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    pageItem.mediaKind == VaultMediaKind.VIDEO -> {
                        val playbackUri = state.playbackUri
                        val canPlay = active &&
                            pageItem.id == state.item.id &&
                            playbackUri != null
                        if (canPlay && playbackUri != null) {
                            VideoPlayer(
                                uri = playbackUri,
                                playWhenReady = true,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            if (pageItem.id == state.item.id && state.image != null) {
                                Image(
                                    bitmap = state.image,
                                    contentDescription = pageItem.displayName,
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
                    page == initial && state.image != null -> Image(
                        bitmap = state.image,
                        contentDescription = pageItem.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    else -> Text(pageItem.displayName, color = Color.White)
                }
            }
        }
        if (state.busy) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        }
        Row(
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
                modifier = Modifier.padding(end = 12.dp),
            )
        }
        Column(
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
            }
        }
    }
}

expect fun decodeVaultImage(bytes: ByteArray): androidx.compose.ui.graphics.ImageBitmap?
