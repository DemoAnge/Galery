package com.example.galeria.ui

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.Gravity
import androidx.core.net.toUri
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
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
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    val wantPlay = remember { mutableStateOf(playWhenReady) }
    wantPlay.value = playWhenReady

    LaunchedEffect(playWhenReady, videoView) {
        val view = videoView ?: return@LaunchedEffect
        if (playWhenReady) view.start() else if (view.isPlaying) view.pause()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> videoView?.pause()
                Lifecycle.Event.ON_START -> if (wantPlay.value) videoView?.start()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { viewContext ->
                val video = VideoView(viewContext)
                val controller = MediaController(viewContext)
                controller.setAnchorView(video)
                video.setMediaController(controller)
                video.setOnErrorListener { _, _, _ ->
                    error = "Error al reproducir el video"
                    true
                }
                video.setOnPreparedListener { player ->
                    try {
                        player.isLooping = false
                        player.setVideoScalingMode(
                            android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT,
                        )
                        if (wantPlay.value) video.start()
                    } catch (_: Throwable) {
                        error = "Error al reproducir el video"
                    }
                }
                try {
                    video.setVideoURI(playbackUri(uri))
                } catch (_: Throwable) {
                    error = "Error al reproducir el video"
                }
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
            update = { layout ->
                videoView = layout.getChildAt(0) as? VideoView
            },
            onRelease = { layout ->
                (layout.getChildAt(0) as? VideoView)?.stopPlayback()
                videoView = null
            },
            modifier = Modifier.fillMaxSize(),
        )
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
