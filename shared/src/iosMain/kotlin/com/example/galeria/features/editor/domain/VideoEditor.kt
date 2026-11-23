package com.example.galeria.features.editor.domain

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetExportPresetHighestQuality
import platform.AVFoundation.AVAssetExportPresetPassthrough
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVURLAsset
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeRangeMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Photos.PHAsset
import platform.Photos.PHImageManager
import platform.Photos.PHVideoRequestOptions
import platform.Photos.PHVideoRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHVideoRequestOptionsVersionCurrent
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

actual class VideoEditor actual constructor() {
    actual fun apply(sourceUri: String, edit: VideoEdit, durationMs: Long): String {
        val clamped = edit.clamped(durationMs)
        val output = tempOutputPath()
        NSFileManager.defaultManager.removeItemAtPath(output, error = null)
        val asset = loadAsset(sourceUri)
        exportTrimmed(asset, output, clamped.startMs, clamped.endMs)
        return output
    }
}

actual fun deleteEditedVideoFile(path: String) {
    if (path.isBlank()) return
    NSFileManager.defaultManager.removeItemAtPath(path, error = null)
}

private fun tempOutputPath(): String =
    NSTemporaryDirectory() + "galeria-edit-" + NSUUID.UUID().UUIDString + ".mp4"

@OptIn(ExperimentalForeignApi::class)
private fun loadAsset(sourceUri: String): AVAsset {
    if (sourceUri.startsWith("file:", ignoreCase = true) || sourceUri.startsWith("/")) {
        val url = if (sourceUri.startsWith("file:", ignoreCase = true)) {
            NSURL.URLWithString(sourceUri)
        } else {
            NSURL.fileURLWithPath(sourceUri)
        }
        check(url != null) { "Video edit failed" }
        return AVURLAsset.URLAssetWithURL(url, options = null)
    }
    val fetch = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(sourceUri), null)
    val phAsset = fetch.firstObject as? PHAsset ?: error("Video edit failed")
    val options = PHVideoRequestOptions().apply {
        setNetworkAccessAllowed(true)
        setVersion(PHVideoRequestOptionsVersionCurrent)
        setDeliveryMode(PHVideoRequestOptionsDeliveryModeHighQualityFormat)
    }
    val semaphore = dispatch_semaphore_create(0)
    var result: AVAsset? = null
    PHImageManager.defaultManager.requestAVAssetForVideo(
        phAsset,
        options = options,
    ) { avAsset, _, _ ->
        result = avAsset
        dispatch_semaphore_signal(semaphore)
    }
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
    return result ?: error("Video edit failed")
}

@OptIn(ExperimentalForeignApi::class)
private fun exportTrimmed(asset: AVAsset, outputPath: String, startMs: Long, endMs: Long) {
    val span = (endMs - startMs).coerceAtLeast(0L)
    fun run(preset: String) {
        val session = AVAssetExportSession(asset = asset, presetName = preset)
            ?: error("Video edit failed")
        session.outputURL = NSURL.fileURLWithPath(outputPath)
        session.outputFileType = AVFileTypeMPEG4
        if (span > 0L) {
            session.timeRange = CMTimeRangeMake(
                start = CMTimeMake(startMs, 1000),
                duration = CMTimeMake(span, 1000),
            )
        }
        val semaphore = dispatch_semaphore_create(0)
        session.exportAsynchronouslyWithCompletionHandler {
            dispatch_semaphore_signal(semaphore)
        }
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
        check(session.status == AVAssetExportSessionStatusCompleted) { "Video edit failed" }
    }
    try {
        run(AVAssetExportPresetPassthrough)
    } catch (_: Exception) {
        NSFileManager.defaultManager.removeItemAtPath(outputPath, error = null)
        run(AVAssetExportPresetHighestQuality)
    }
}
