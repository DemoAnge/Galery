package com.example.galeria.features.gallery.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.example.galeria.features.gallery.domain.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

expect fun decodePublicImage(publicUri: String, maxEdge: Int): ImageBitmap?

expect fun loadEditJpeg(publicUri: String, maxEdge: Int): ByteArray?

@Composable
internal fun MediaThumb(
    item: MediaItem?,
    modifier: Modifier = Modifier,
    maxEdge: Int = 256,
) {
    if (item == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
        return
    }
    var thumbnail by remember(item.publicUri, maxEdge) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(item.publicUri, maxEdge) {
        thumbnail = runCatching {
            withContext(Dispatchers.Default) {
                decodePublicImage(item.publicUri, maxEdge)
            }
        }.getOrNull()
    }
    val image = thumbnail
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = item.displayName,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
internal fun MediaThumbFill(item: MediaItem?, modifier: Modifier = Modifier) {
    MediaThumb(item = item, modifier = modifier.fillMaxSize())
}
