package com.example.galeria.features.gallery.presentation

import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class LibrarySyncTest {

    @Test
    fun fingerprintIgnoresItemOrder() {
        val a = photo("a", captured = 1L)
        val b = photo("b", captured = 2L)
        assertEquals(libraryFingerprint(listOf(a, b)), libraryFingerprint(listOf(b, a)))
    }

    @Test
    fun fingerprintChangesWhenAPhotoIsAdded() {
        val a = photo("a")
        val b = photo("b")
        assertNotEquals(libraryFingerprint(listOf(a)), libraryFingerprint(listOf(a, b)))
    }

    @Test
    fun debounceWaitsUnlessTheBurstIsTooLong() {
        assertEquals(LIBRARY_DEBOUNCE_MS, libraryRefreshDelayMs(0L))
        assertEquals(LIBRARY_DEBOUNCE_MS, libraryRefreshDelayMs(500L))
        assertEquals(0L, libraryRefreshDelayMs(LIBRARY_MAX_WAIT_MS))
        assertEquals(0L, libraryRefreshDelayMs(LIBRARY_MAX_WAIT_MS + 10L))
    }

    @Test
    fun openViewerKeepsTheCurrentPhotoWhenLibraryGainsItems() {
        val session = ViewerSession(items = listOf(photo("a"), photo("b")), index = 1)
        val available = listOf(photo("a"), photo("b"), photo("c")).associateBy { it.id }
        val next = viewingAfterLibraryChange(session, available)
        assertEquals(listOf("a", "b"), next?.items?.map { it.id })
        assertEquals("b", next?.item?.id)
    }

    @Test
    fun openViewerDropsDeletedPhotos() {
        val session = ViewerSession(items = listOf(photo("a"), photo("b"), photo("c")), index = 0)
        val next = viewingAfterLibraryChange(session, mapOf("b" to photo("b"), "c" to photo("c")))
        assertEquals(listOf("b", "c"), next?.items?.map { it.id })
        assertEquals("b", next?.item?.id)
        assertNull(next?.image)
    }

    private fun photo(id: String, captured: Long = 1_700_000_000_000L): MediaItem = MediaItem(
        id = id,
        publicUri = "content://media/$id",
        displayName = "$id.jpg",
        mediaKind = GalleryMediaKind.IMAGE,
        capturedAtEpochMs = captured,
        sizeBytes = 10L,
    )
}
