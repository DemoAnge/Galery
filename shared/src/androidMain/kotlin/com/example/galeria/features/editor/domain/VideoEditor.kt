package com.example.galeria.features.editor.domain

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import androidx.core.net.toUri
import com.example.galeria.core.AndroidAppContext
import java.io.File
import java.nio.ByteBuffer

actual class VideoEditor actual constructor() {
    actual fun apply(sourceUri: String, edit: VideoEdit, durationMs: Long): String {
        val clamped = edit.clamped(durationMs)
        val output = File.createTempFile("galeria-edit-", ".mp4", AndroidAppContext.require().cacheDir)
        try {
            copyTrimmed(
                source = playbackUri(sourceUri),
                output = output,
                startMs = clamped.startMs,
                endMs = clamped.endMs,
                extraRotation = clamped.rotationDegrees,
                includeAudio = !clamped.mute,
            )
            return output.absolutePath
        } catch (e: Exception) {
            output.delete()
            throw e
        }
    }
}

actual fun deleteEditedVideoFile(path: String) {
    if (path.isBlank()) return
    File(path).delete()
}

private fun playbackUri(uri: String): Uri = when {
    uri.startsWith("content:", ignoreCase = true) -> uri.toUri()
    uri.startsWith("file:", ignoreCase = true) -> uri.toUri()
    else -> Uri.fromFile(File(uri))
}

private fun copyTrimmed(
    source: Uri,
    output: File,
    startMs: Long,
    endMs: Long,
    extraRotation: Int,
    includeAudio: Boolean,
) {
    val context = AndroidAppContext.require()
    val extractor = MediaExtractor()
    val pfd = context.contentResolver.openFileDescriptor(source, "r")
        ?: error("Video edit failed")
    var muxer: MediaMuxer? = null
    var started = false
    try {
        extractor.setDataSource(pfd.fileDescriptor)
        val startUs = startMs.coerceAtLeast(0L) * 1000L
        val endUs = endMs.coerceAtLeast(startMs) * 1000L
        val indexMap = HashMap<Int, Int>()
        muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var hasVideo = false
        var maxInput = 256 * 1024
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            val video = mime.startsWith("video/")
            val audio = mime.startsWith("audio/")
            if (!video && !audio) continue
            if (audio && !includeAudio) continue
            if (video) hasVideo = true
            if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                maxInput = maxOf(maxInput, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
            }
            if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                format.setInteger(MediaFormat.KEY_ROTATION, 0)
            }
            extractor.selectTrack(i)
            indexMap[i] = muxer.addTrack(format)
        }
        check(hasVideo && indexMap.isNotEmpty()) { "Video edit failed" }
        muxer.setOrientationHint(normalizedRotation(originalRotation(source) + extraRotation))
        muxer.start()
        started = true
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        val buffer = ByteBuffer.allocateDirect(maxInput.coerceIn(64 * 1024, 2 * 1024 * 1024))
        val info = MediaCodec.BufferInfo()
        var offsetUs = -1L
        while (true) {
            val track = extractor.sampleTrackIndex
            if (track < 0) break
            val dst = indexMap[track]
            val time = extractor.sampleTime
            if (dst == null || time < 0L || time > endUs) {
                extractor.advance()
                continue
            }
            buffer.clear()
            info.offset = 0
            info.size = extractor.readSampleData(buffer, 0)
            if (info.size < 0) break
            if (offsetUs < 0L) offsetUs = time
            info.presentationTimeUs = (time - offsetUs).coerceAtLeast(0L)
            info.flags = extractor.sampleFlags
            muxer.writeSampleData(dst, buffer, info)
            extractor.advance()
        }
        muxer.stop()
        started = false
    } finally {
        val active = muxer
        if (active != null) {
            if (started) runCatching { active.stop() }
            runCatching { active.release() }
        }
        extractor.release()
        pfd.close()
    }
}

private fun originalRotation(uri: Uri): Int {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(AndroidAppContext.require(), uri)
        val raw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
        normalizedRotation(raw?.toIntOrNull() ?: 0)
    } catch (_: Exception) {
        0
    } finally {
        runCatching { retriever.release() }
    }
}
