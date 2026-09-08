package com.example.galeria.ui

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
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.Photos.PHAsset
import platform.Photos.PHImageManager
import platform.Photos.PHVideoRequestOptions
import platform.Photos.PHVideoRequestOptionsDeliveryModeAutomatic
import platform.Photos.PHVideoRequestOptionsVersionCurrent
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    uri: String,
    modifier: Modifier,
    playWhenReady: Boolean,
) {
    val player = remember { AVPlayer() }
    var error by remember(uri) { mutableStateOf<String?>(null) }
    val wantsPlay = remember { mutableStateOf(playWhenReady) }
    wantsPlay.value = playWhenReady

    DisposableEffect(uri) {
        val handle = attachSource(player, uri) { failed ->
            if (failed) {
                error = "Error al reproducir el video"
            } else if (wantsPlay.value) {
                player.play()
            }
        }
        onDispose {
            handle.cancel()
            player.pause()
            player.replaceCurrentItemWithPlayerItem(null)
        }
    }
    LaunchedEffect(playWhenReady) {
        if (playWhenReady) player.play() else player.pause()
    }
    DisposableEffect(player) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification,
            `object` = null,
            queue = null,
        ) { _ ->
            error = "Error al reproducir el video"
        }
        onDispose {
            center.removeObserver(observer)
        }
    }
    Box(modifier.background(Color.Black)) {
        UIKitViewController(
            factory = {
                AVPlayerViewController().apply {
                    this.player = player
                    showsPlaybackControls = true
                    videoGravity = AVLayerVideoGravityResizeAspect
                    view.backgroundColor = UIColor.blackColor
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { controller ->
                val playerController = controller as AVPlayerViewController
                playerController.player = player
            },
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

@OptIn(ExperimentalForeignApi::class)
internal fun attachSource(
    player: AVPlayer,
    uri: String,
    onReady: (failed: Boolean) -> Unit,
): PlaybackHandle {
    if (uri.startsWith("file:", ignoreCase = true) || uri.startsWith("/")) {
        val url = if (uri.startsWith("file:", ignoreCase = true)) {
            NSURL.URLWithString(uri)
        } else {
            NSURL.fileURLWithPath(uri)
        }
        if (url == null) {
            onReady(true)
            return PlaybackHandle {}
        }
        val item = AVPlayerItem(uRL = url)
        player.replaceCurrentItemWithPlayerItem(item)
        onReady(item.status == AVPlayerItemStatusFailed)
        return PlaybackHandle {}
    }
    val fetch = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(uri), null)
    val asset = fetch.firstObject as? PHAsset
    if (asset == null) {
        onReady(true)
        return PlaybackHandle {}
    }
    val options = PHVideoRequestOptions().apply {
        setNetworkAccessAllowed(true)
        setVersion(PHVideoRequestOptionsVersionCurrent)
        setDeliveryMode(PHVideoRequestOptionsDeliveryModeAutomatic)
    }
    val requestId = PHImageManager.defaultManager.requestAVAssetForVideo(
        asset,
        options = options,
    ) { avAsset, _, _ ->
        if (avAsset == null) {
            onReady(true)
            return@requestAVAssetForVideo
        }
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(asset = avAsset))
        onReady(false)
    }
    return PlaybackHandle {
        PHImageManager.defaultManager.cancelImageRequest(requestId)
    }
}

internal class PlaybackHandle(val cancel: () -> Unit)
