package com.example.galeria.features.editor.domain

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVAssetExportPresetHighestQuality
import platform.AVFoundation.AVAssetExportPresetPassthrough
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMutableComposition
import platform.AVFoundation.AVMutableVideoComposition
import platform.AVFoundation.AVMutableVideoCompositionInstruction
import platform.AVFoundation.AVMutableVideoCompositionLayerInstruction
import platform.AVFoundation.AVURLAsset
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeRangeMake
import platform.CoreMedia.kCMTimeZero
import kotlin.math.PI
import kotlin.math.atan2
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
        exportEdited(asset, output, clamped)
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
private fun exportEdited(asset: AVAsset, outputPath: String, edit: VideoEdit) {
    if (edit.rotationDegrees == 0 && !edit.mute) {
        exportTrimmed(asset, outputPath, edit.startMs, edit.endMs)
        return
    }
    exportComposed(asset, outputPath, edit)
}

@OptIn(ExperimentalForeignApi::class)
private fun exportTrimmed(asset: AVAsset, outputPath: String, startMs: Long, endMs: Long) {
    val span = (endMs - startMs).coerceAtLeast(0L)
    fun run(preset: String) {
        exportSession(asset, outputPath, preset) { session ->
            if (span > 0L) {
                session.timeRange = CMTimeRangeMake(
                    start = CMTimeMake(startMs, 1000),
                    duration = CMTimeMake(span, 1000),
                )
            }
        }
    }
    try {
        run(AVAssetExportPresetPassthrough)
    } catch (_: Exception) {
        NSFileManager.defaultManager.removeItemAtPath(outputPath, error = null)
        run(AVAssetExportPresetHighestQuality)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun exportComposed(asset: AVAsset, outputPath: String, edit: VideoEdit) {
    val sourceVideo = asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? AVAssetTrack
        ?: error("Video edit failed")
    val span = (edit.endMs - edit.startMs).coerceAtLeast(0L)
    val range = CMTimeRangeMake(
        start = CMTimeMake(edit.startMs, 1000),
        duration = CMTimeMake(span, 1000),
    )
    val composition = AVMutableComposition()
    val videoTrack = composition.addMutableTrackWithMediaType(
        mediaType = AVMediaTypeVideo,
        preferredTrackID = 0,
    ) ?: error("Video edit failed")
    val inserted = videoTrack.insertTimeRange(
        timeRange = range,
        ofTrack = sourceVideo,
        atTime = kCMTimeZero,
        error = null,
    )
    check(inserted) { "Video edit failed" }
    if (!edit.mute) {
        val sourceAudio = asset.tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as? AVAssetTrack
        if (sourceAudio != null) {
            val audioTrack = composition.addMutableTrackWithMediaType(
                mediaType = AVMediaTypeAudio,
                preferredTrackID = 0,
            )
            audioTrack?.insertTimeRange(
                timeRange = range,
                ofTrack = sourceAudio,
                atTime = kCMTimeZero,
                error = null,
            )
        }
    }
    if (edit.rotationDegrees == 0) {
        try {
            exportSession(composition, outputPath, AVAssetExportPresetPassthrough) {}
        } catch (_: Exception) {
            NSFileManager.defaultManager.removeItemAtPath(outputPath, error = null)
            exportSession(composition, outputPath, AVAssetExportPresetHighestQuality) {}
        }
        return
    }
    val (width, height) = sourceVideo.naturalSize.useContents { width to height }
    val totalRotation = normalizedRotation(trackRotationDegrees(sourceVideo) + edit.rotationDegrees)
    val transform = transformFor(totalRotation, width, height)
    val (renderW, renderH) = renderSize(totalRotation, width, height)
    val layer = AVMutableVideoCompositionLayerInstruction.videoCompositionLayerInstructionWithAssetTrack(videoTrack)
    layer.setTransform(transform, atTime = kCMTimeZero)
    val instruction = AVMutableVideoCompositionInstruction.videoCompositionInstruction()
    instruction.timeRange = CMTimeRangeMake(kCMTimeZero, CMTimeMake(span, 1000))
    instruction.layerInstructions = listOf(layer)
    val videoComp = AVMutableVideoComposition.videoComposition()
    videoComp.instructions = listOf(instruction)
    val fps = sourceVideo.nominalFrameRate.toInt().let { if (it in 1..60) it else 30 }
    videoComp.frameDuration = CMTimeMake(1, fps)
    videoComp.renderSize = CGSizeMake(renderW, renderH)
    exportSession(composition, outputPath, AVAssetExportPresetHighestQuality) { session ->
        session.videoComposition = videoComp
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun exportSession(
    asset: AVAsset,
    outputPath: String,
    preset: String,
    configure: (AVAssetExportSession) -> Unit,
) {
    val session = AVAssetExportSession(asset = asset, presetName = preset)
        ?: error("Video edit failed")
    session.outputURL = NSURL.fileURLWithPath(outputPath)
    session.outputFileType = AVFileTypeMPEG4
    configure(session)
    val semaphore = dispatch_semaphore_create(0)
    session.exportAsynchronouslyWithCompletionHandler {
        dispatch_semaphore_signal(semaphore)
    }
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
    check(session.status == AVAssetExportSessionStatusCompleted) { "Video edit failed" }
}

@OptIn(ExperimentalForeignApi::class)
private fun trackRotationDegrees(track: AVAssetTrack): Int {
    return track.preferredTransform.useContents {
        val degrees = (atan2(b, a) * 180.0 / PI).toInt()
        normalizedRotation(degrees)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun transformFor(degrees: Int, width: Double, height: Double): CValue<CGAffineTransform> {
    return when (normalizedRotation(degrees)) {
        90 -> CGAffineTransformMake(0.0, 1.0, -1.0, 0.0, height, 0.0)
        180 -> CGAffineTransformMake(-1.0, 0.0, 0.0, -1.0, width, height)
        270 -> CGAffineTransformMake(0.0, -1.0, 1.0, 0.0, 0.0, width)
        else -> CGAffineTransformMake(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
    }
}

private fun renderSize(degrees: Int, width: Double, height: Double): Pair<Double, Double> {
    return if (normalizedRotation(degrees) == 90 || normalizedRotation(degrees) == 270) {
        height to width
    } else {
        width to height
    }
}
