package com.example.galeria.ui

import android.graphics.Color as AndroidColor
import android.media.MediaPlayer
import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

@Composable
actual fun VideoPlayer(
    uri: String,
    modifier: Modifier,
    playWhenReady: Boolean,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var error by remember(uri) { mutableStateOf<String?>(null) }
    var prepared by remember(uri) { mutableStateOf(false) }
    var released by remember(uri) { mutableStateOf(false) }
    var videoView by remember(uri) { mutableStateOf<VideoView?>(null) }

    fun safeStart(view: VideoView?) {
        if (view == null || released || !prepared || error != null) return
        runCatching { view.start() }
    }

    fun safePause(view: VideoView?) {
        if (view == null || released) return
        runCatching {
            if (view.isPlaying) view.pause()
        }
    }

    LaunchedEffect(playWhenReady, videoView, prepared, error) {
        val view = videoView ?: return@LaunchedEffect
        if (playWhenReady) safeStart(view) else safePause(view)
    }
    DisposableEffect(lifecycleOwner, uri) {
        val observer = LifecycleEventObserver { _, event ->
            val view = videoView
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> safePause(view)
                Lifecycle.Event.ON_RESUME -> if (playWhenReady) safeStart(view)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier.background(Color.Black)) {
        key(uri) {
            AndroidView(
                factory = { viewContext ->
                    prepared = false
                    released = false
                    val video = VideoView(viewContext)
                    video.setMediaController(null)
                    video.setOnErrorListener { _, _, _ ->
                        prepared = false
                        error = "Error al reproducir el video"
                        true
                    }
                    video.setOnPreparedListener { player ->
                        if (released) return@setOnPreparedListener
                        try {
                            player.isLooping = false
                            player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                            prepared = true
                            if (playWhenReady) video.start()
                        } catch (_: Throwable) {
                            prepared = false
                            error = "Error al reproducir el video"
                        }
                    }
                    runCatching { video.setVideoURI(playbackUri(uri)) }
                        .onFailure { error = "Error al reproducir el video" }
                    videoView = video
                    FrameLayout(viewContext).apply {
                        setBackgroundColor(AndroidColor.BLACK)
                        addView(
                            video,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                Gravity.CENTER,
                            ),
                        )
                    }
                },
                onRelease = { layout ->
                    released = true
                    prepared = false
                    val video = layout.getChildAt(0) as? VideoView
                    runCatching {
                        video?.setOnPreparedListener(null)
                        video?.setOnCompletionListener(null)
                        video?.setMediaController(null)
                        video?.stopPlayback()
                    }
                    videoView = null
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        val message = error
        if (message != null) {
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private fun playbackUri(uri: String): Uri {
    return when {
        uri.startsWith("content:", ignoreCase = true) -> uri.toUri()
        uri.startsWith("file:", ignoreCase = true) -> uri.toUri()
        uri.startsWith("http", ignoreCase = true) -> uri.toUri()
        else -> Uri.fromFile(File(uri))
    }
}
