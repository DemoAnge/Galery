package com.example.galeria.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVKit.AVPlayerViewController
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoMutePreview(
    uri: String,
    modifier: Modifier,
    playing: Boolean,
    onVideoSize: ((width: Int, height: Int) -> Unit)?,
) {
    val player = remember { AVPlayer() }
    player.volume = 0f
    allowMutePreviewToMixWithOtherAudio()
    DisposableEffect(uri) {
        val handle = attachSource(player, uri) { failed ->
            if (!failed && playing) player.play()
        }
        onDispose {
            handle.cancel()
            player.pause()
            player.replaceCurrentItemWithPlayerItem(null)
        }
    }
    LaunchedEffect(playing) {
        if (playing) player.play() else player.pause()
    }
    UIKitViewController(
        factory = {
            AVPlayerViewController().apply {
                this.player = player
                showsPlaybackControls = false
                videoGravity = AVLayerVideoGravityResizeAspect
                view.backgroundColor = UIColor.blackColor
            }
        },
        modifier = modifier,
        update = { controller ->
            (controller as AVPlayerViewController).player = player
        },
    )
}

private fun allowMutePreviewToMixWithOtherAudio() {
    runCatching {
        AVAudioSession.sharedInstance().setCategory(
            AVAudioSessionCategoryAmbient,
            withOptions = AVAudioSessionCategoryOptionMixWithOthers,
            error = null,
        )
    }
}
