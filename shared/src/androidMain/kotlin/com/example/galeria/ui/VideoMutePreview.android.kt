package com.example.galeria.ui

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import java.io.File

@Composable
actual fun VideoMutePreview(
    uri: String,
    modifier: Modifier,
    playing: Boolean,
    onVideoSize: ((width: Int, height: Int) -> Unit)?,
) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                disableAudioFocusForMutePreview()
                setOnPreparedListener { player ->
                    try {
                        player.muteWithoutStealingAudio(context)
                        player.isLooping = true
                        player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                        val width = player.videoWidth
                        val height = player.videoHeight
                        if (width > 0 && height > 0) {
                            onVideoSize?.invoke(width, height)
                        }
                        if (playing) start()
                    } catch (_: Throwable) {
                    }
                }
                setOnErrorListener { _, _, _ -> true }
                try {
                    setVideoURI(
                        if (uri.startsWith("content:", true) ||
                            uri.startsWith("file:", true) ||
                            uri.startsWith("http", true)
                        ) {
                            uri.toUri()
                        } else {
                            android.net.Uri.fromFile(File(uri))
                        },
                    )
                } catch (_: Throwable) {
                }
            }
        },
        update = { view ->
            if (playing) {
                if (!view.isPlaying) view.start()
            } else if (view.isPlaying) {
                view.pause()
            }
        },
        onRelease = { view ->
            view.stopPlayback()
        },
        modifier = modifier,
    )
}

private fun VideoView.disableAudioFocusForMutePreview() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching { setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE) }
    }
}

private fun MediaPlayer.muteWithoutStealingAudio(context: Context) {
    setVolume(0f, 0f)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        @Suppress("DEPRECATION")
        runCatching {
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            manager?.abandonAudioFocus(null)
        }
    }
}
