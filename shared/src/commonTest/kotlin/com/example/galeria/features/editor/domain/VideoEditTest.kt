package com.example.galeria.features.editor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoEditTest {

    @Test
    fun clampKeepsAMinimumSpanAndFillsAnOpenEnd() {
        val duration = 10_000L
        val clamped = VideoEdit(startMs = -50L, endMs = 0L).clamped(duration)
        assertEquals(0L, clamped.startMs)
        assertEquals(duration, clamped.endMs)

        val short = VideoEdit(startMs = 9_900L, endMs = 9_910L).clamped(duration)
        assertEquals(9_800L, short.startMs)
        assertEquals(duration, short.endMs)
        assertTrue(short.endMs - short.startMs >= MIN_TRIM_MS)
    }

    @Test
    fun identityMeansFullClipWithoutRotateOrMute() {
        val duration = 5_000L
        assertTrue(VideoEdit().isIdentity(duration))
        assertFalse(VideoEdit(startMs = 500L, endMs = 4_000L).isIdentity(duration))
        assertFalse(VideoEdit(rotationDegrees = 90).isIdentity(duration))
        assertFalse(VideoEdit(mute = true).isIdentity(duration))
    }

    @Test
    fun rotationSnapsToRightAngles() {
        assertEquals(0, normalizedRotation(10))
        assertEquals(90, normalizedRotation(95))
        assertEquals(180, normalizedRotation(-180))
        assertEquals(270, VideoEdit().rotatedBy(-90).rotationDegrees)
        assertEquals(0, VideoEdit(rotationDegrees = 270).rotatedBy(90).rotationDegrees)
    }
}
