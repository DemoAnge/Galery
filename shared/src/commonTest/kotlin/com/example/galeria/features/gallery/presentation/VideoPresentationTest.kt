package com.example.galeria.features.gallery.presentation

import com.example.galeria.features.gallery.domain.Album
import com.example.galeria.features.gallery.domain.AlbumId
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.features.gallery.domain.displaySize
import com.example.galeria.ui.playableVideoExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoPresentationTest {

    @Test
    fun formatDurationUsesMinutesAndHours() {
        assertEquals("0:00", formatDuration(0L))
        assertEquals("0:05", formatDuration(5_000L))
        assertEquals("1:01", formatDuration(61_000L))
        assertEquals("1:02:03", formatDuration(3_723_000L))
    }

    @Test
    fun libraryAlbumsPlacesVideosNearTheTop() {
        val photo = media("p", GalleryMediaKind.IMAGE)
        val video = media("v", GalleryMediaKind.VIDEO)
        val albums = listOf(Album(AlbumId.CAMERA, "Cámara", listOf(photo, video)))
        val library = buildLibraryAlbums(albums, listOf(photo, video), favorites = emptySet())
        assertEquals(VIDEOS_COLLECTION_TITLE, library.first().title)
        assertEquals(listOf(video), library.first().items)
    }

    @Test
    fun playableExtensionKeepsContainer() {
        assertEquals(".mov", playableVideoExtension("clip.MOV"))
        assertEquals(".mp4", playableVideoExtension("clip"))
        assertEquals(".webm", playableVideoExtension("note.webm"))
    }

    @Test
    fun tileAspectRatioUsesRealDimensions() {
        val wide = media("p", GalleryMediaKind.IMAGE).copy(widthPx = 4000, heightPx = 3000)
        assertTrue(kotlin.math.abs(wide.tileAspectRatio() - 4f / 3f) < 0.01f)
    }

    @Test
    fun videoTileFollowsPortraitOrLandscape() {
        val portrait = media("v-port", GalleryMediaKind.VIDEO).copy(widthPx = 1080, heightPx = 1920)
        val landscape = media("v-land", GalleryMediaKind.VIDEO).copy(widthPx = 1920, heightPx = 1080)
        assertTrue(portrait.tileAspectRatio() < 1f)
        assertTrue(landscape.tileAspectRatio() > 1f)
    }

    @Test
    fun displaySizeSwapsWhenVideoIsRotated() {
        val (width, height) = displaySize(1920, 1080, 90)
        assertEquals(1080, width)
        assertEquals(1920, height)
        val upright = displaySize(1920, 1080, 0)
        assertEquals(1920, upright.first)
        assertEquals(1080, upright.second)
    }

    @Test
    fun classifyVideoAlbumsGroupsBySourceAndDropsPhotos() {
        val cameraPhoto = media("p-cam", GalleryMediaKind.IMAGE).copy(albumId = AlbumId.CAMERA)
        val cameraVideo = media("v-cam", GalleryMediaKind.VIDEO).copy(albumId = AlbumId.CAMERA)
        val whatsappVideo = media("v-wa", GalleryMediaKind.VIDEO).copy(albumId = AlbumId.WHATSAPP)
        val downloadVideo = media("v-dl", GalleryMediaKind.VIDEO).copy(albumId = AlbumId.DOWNLOADS)
        val otherVideo = media("v-other", GalleryMediaKind.VIDEO).copy(albumId = AlbumId.OTHER)
        val albums = listOf(
            Album(AlbumId.CAMERA, "Cámara", listOf(cameraPhoto, cameraVideo)),
            Album(AlbumId.WHATSAPP, "WhatsApp", listOf(whatsappVideo)),
            Album(AlbumId.DOWNLOADS, "Descargas", listOf(downloadVideo)),
            Album(AlbumId.OTHER, "Instagram", listOf(otherVideo)),
            Album(AlbumId.OTHER, "Viaje", listOf(cameraPhoto), customId = "custom-1"),
        )
        val classified = classifyVideoAlbums(
            albums,
            listOf(cameraPhoto, cameraVideo, whatsappVideo, downloadVideo, otherVideo),
        )
        assertEquals(listOf("Cámara", "WhatsApp", "Descargas", "Instagram"), classified.map { it.title })
        assertEquals(listOf(cameraVideo), classified[0].items)
        assertTrue(classified.none { album -> album.items.any { it.mediaKind != GalleryMediaKind.VIDEO } })
        assertTrue(classified.none { it.customId != null })
        val groups = videoSourceGroups(classified)
        assertEquals(classified.map { it.title }, groups.map { it.label })
        assertEquals(ALL_VIDEOS_KEY, "all")
        assertEquals(":CAMERA:Cámara", videoAlbumKey(classified[0]))
    }

    @Test
    fun classifyVideoAlbumsKeepsUnassignedVideosInOtros() {
        val known = media("v-cam", GalleryMediaKind.VIDEO).copy(albumId = AlbumId.CAMERA)
        val stray = media("v-stray", GalleryMediaKind.VIDEO)
        val classified = classifyVideoAlbums(
            listOf(Album(AlbumId.CAMERA, "Cámara", listOf(known))),
            listOf(known, stray),
        )
        assertEquals(listOf("Cámara", "Otros"), classified.map { it.title })
        assertEquals(listOf(stray), classified.last().items)
    }

    @Test
    fun memoryCardsIncludeVideosCollection() {
        val videos = listOf(media("v1", GalleryMediaKind.VIDEO), media("v2", GalleryMediaKind.VIDEO))
        val cards = memoryCards(videos, albums = emptyList(), favorites = emptySet())
        assertTrue(cards.any { it.title == "Revive el momento" && it.items.size == 2 })
        assertTrue(cards.size >= 2)
        assertTrue(cards.map { it.id }.toSet().size == cards.size)
    }

    @Test
    fun memoryCardsBuildSwipeableDayDumps() {
        val day = 86_400_000L
        val items = (0 until 12).map { index ->
            media("p$index", GalleryMediaKind.IMAGE).copy(
                displayName = "p$index.jpg",
                capturedAtEpochMs = 1_700_000_000_000L - (index / 4) * day,
            )
        }
        val cards = memoryCards(items, albums = emptyList(), favorites = emptySet())
        assertTrue(cards.size >= 3)
        assertTrue(cards.count { it.id.startsWith("day-") } >= 3)
        assertTrue(cards.any { it.id == "weekend" })
    }

    @Test
    fun stackNearbyDoesNotHideADumpBehindOneTile() {
        val burst = (0 until 6).map { index ->
            media("b$index", GalleryMediaKind.IMAGE).copy(
                displayName = "b$index.jpg",
                capturedAtEpochMs = 1_700_000_000_000L + index,
            )
        }
        val stacks = stackNearby(burst)
        assertEquals(6, stacks.size)
    }

    @Test
    fun singlePhotoOfTheDayIsFeatured() {
        val photo = media("solo", GalleryMediaKind.IMAGE)
        assertEquals(FeedCardRole.SUPER, photo.feedCardRole(indexInGroup = 0, groupSize = 1))
        assertEquals(FeedCardRole.TILE, photo.feedCardRole(indexInGroup = 1, groupSize = 3))
    }

    @Test
    fun collectionKeepsLargeAndSuperCards() {
        val portraits = (0 until 12).map { index ->
            media("c$index", GalleryMediaKind.IMAGE).copy(widthPx = 1080, heightPx = 1920)
        }
        val roles = portraits.mapIndexed { index, item -> item.feedCardRole(index, portraits.size) }
        assertTrue(roles.contains(FeedCardRole.SUPER) || roles.contains(FeedCardRole.GRANDE))
        assertTrue(roles.none { it.isFullLine() })
        assertTrue(roles.any { it == FeedCardRole.TILE })
        val grande = portraits[0].cardAspectRatio(FeedCardRole.GRANDE)
        val superCard = portraits[0].cardAspectRatio(FeedCardRole.SUPER)
        val tile = portraits[0].tileAspectRatio()
        assertTrue(grande > superCard)
        assertTrue(grande <= tile)
    }

    private fun media(id: String, kind: GalleryMediaKind): MediaItem = MediaItem(
        id = id,
        publicUri = "content://media/$id",
        displayName = "$id.mp4",
        mediaKind = kind,
        capturedAtEpochMs = 1_700_000_000_000L,
        sizeBytes = 10L,
        durationMs = 12_000L,
    )
}
