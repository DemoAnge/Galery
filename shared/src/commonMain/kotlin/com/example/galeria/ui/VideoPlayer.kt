package com.example.galeria.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reproductor de video local. [uri] es un content://, file://, ruta de archivo
 * o el identificador de PHAsset en iOS.
 */
@Composable
expect fun VideoPlayer(
    uri: String,
    modifier: Modifier = Modifier,
    playWhenReady: Boolean = true,
)

@Composable
expect fun VideoMutePreview(
    uri: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    onVideoSize: ((width: Int, height: Int) -> Unit)? = null,
)

internal fun playableVideoExtension(displayName: String): String {
    val name = displayName.lowercase()
    return when {
        name.endsWith(".mov") -> ".mov"
        name.endsWith(".webm") -> ".webm"
        name.endsWith(".mkv") -> ".mkv"
        name.endsWith(".3gp") -> ".3gp"
        name.endsWith(".m4v") -> ".m4v"
        else -> ".mp4"
    }
}
