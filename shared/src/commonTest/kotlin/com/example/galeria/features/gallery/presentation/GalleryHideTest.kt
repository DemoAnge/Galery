package com.example.galeria.features.gallery.presentation

import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GalleryHideTest {

    @Test
    fun afterMovingCurrentPhotoViewerShowsTheNextOne() {
        val items = listOf(photo("a"), photo("b"), photo("c"))
        val session = ViewerSession(items = items, index = 0)
        val next = viewerAfterHide(session, setOf("a"))
        assertEquals(listOf("b", "c"), next?.items?.map { it.id })
        assertEquals(0, next?.index)
        assertEquals("b", next?.item?.id)
    }

    @Test
    fun movingTheLastPhotoKeepsThePreviousOne() {
        val items = listOf(photo("a"), photo("b"))
        val session = ViewerSession(items = items, index = 1)
        val next = viewerAfterHide(session, setOf("b"))
        assertEquals(listOf("a"), next?.items?.map { it.id })
        assertEquals(0, next?.index)
    }

    @Test
    fun movingTheOnlyPhotoClosesTheViewer() {
        val session = ViewerSession(items = listOf(photo("a")), index = 0)
        assertNull(viewerAfterHide(session, setOf("a")))
    }

    @Test
    fun sharePreviewKeepsTheSelectedOrder() {
        val a = photo("a")
        val b = photo("b")
        val ready = GalleryUiState.Ready(
            albums = emptyList(),
            allItems = listOf(b, a),
            viewing = ViewerSession(items = listOf(a, b), index = 0),
            shareSession = ShareSession(
                itemIds = listOf("a", "b"),
                requests = emptyList(),
                targets = emptyList(),
            ),
        )
        assertEquals(listOf("a", "b"), sharePreviewItems(ready).map { it.id })
    }

    private fun photo(id: String): MediaItem = MediaItem(
        id = id,
        publicUri = "content://media/$id",
        displayName = "$id.jpg",
        mediaKind = GalleryMediaKind.IMAGE,
        capturedAtEpochMs = 1_700_000_000_000L,
        sizeBytes = 10L,
    )
}
