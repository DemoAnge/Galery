package com.example.galeria.features.gallery.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanMediaUseCaseTest {

    @Test
    fun googlePhotosFolderIsNotItsOwnAlbum() {
        assertEquals(AlbumId.OTHER, classify("Pictures/Google Photos", "Google Photos"))
        assertEquals("Otros", otherAlbumTitle("Google Photos", "Pictures/Google Photos"))
        assertEquals("Otros", otherAlbumTitle("Google Fotos", null))
    }

    @Test
    fun dovimuMoviesBecomeOwnAlbumNotDownloads() {
        assertEquals(AlbumId.OTHER, classify("Movies/DoVimu", "DoVimu"))
        assertEquals("DoVimu", otherAlbumTitle("DoVimu", "Movies/DoVimu"))
        assertEquals(AlbumId.OTHER, classify("Music/DoVimu", "DoVimu"))
    }

    @Test
    fun downloadedVideosAskForTheFileMetadata() {
        assertTrue(
            videoNeedsFileMetadata(
                durationMs = 0L,
                widthPx = 0,
                heightPx = 0,
                hasTakenDate = false,
                relativePath = "Download",
                bucketName = "Download",
            ),
        )
        assertTrue(
            videoNeedsFileMetadata(
                durationMs = 12_000L,
                widthPx = 1920,
                heightPx = 1080,
                hasTakenDate = false,
                relativePath = "Download/clip.mp4",
                bucketName = "Download",
            ),
        )
        assertFalse(
            videoNeedsFileMetadata(
                durationMs = 12_000L,
                widthPx = 1920,
                heightPx = 1080,
                hasTakenDate = true,
                relativePath = "DCIM/Camera",
                bucketName = "Camera",
            ),
        )
    }

    @Test
    fun embeddedVideoDateUsesTheOriginalCaptureTime() {
        assertEquals(1_710_528_330_000L, parseEmbeddedVideoDate("20240315T184530.000Z"))
        assertEquals(0L, parseEmbeddedVideoDate(""))
        assertEquals("Vacaciones", preferEmbeddedVideoTitle("video.mp4", "Vacaciones"))
        assertEquals("clip.mp4", preferEmbeddedVideoTitle("clip.mp4", "clip"))
    }

    @Test
    fun cameraShotsStayInCameraEvenIfGooglePhotosSavedThem() {
        assertEquals(
            AlbumId.CAMERA,
            classify("DCIM/Camera", "Camera", "com.google.android.apps.photos"),
        )
    }
}
