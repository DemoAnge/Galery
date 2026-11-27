package com.example.galeria.features.share.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShareTargetsTest {

    @Test
    fun storiesNeedASingleImageOrVideo() {
        assertTrue(allowsStoryShare(listOf("image/jpeg")))
        assertTrue(allowsStoryShare(listOf("video/mp4")))
        assertFalse(allowsStoryShare(listOf("image/jpeg", "image/png")))
        assertFalse(allowsStoryShare(emptyList()))
        assertFalse(allowsStoryShare(listOf("application/pdf")))
    }

    @Test
    fun headlineMatchesCountAndKind() {
        assertEquals("Compartir", shareHeadline(0, 0, 0))
        assertEquals("Compartir foto", shareHeadline(1, 1, 0))
        assertEquals("Compartir video", shareHeadline(1, 0, 1))
        assertEquals("Compartir 3 fotos", shareHeadline(3, 3, 0))
        assertEquals("Compartir 2 videos", shareHeadline(2, 0, 2))
        assertEquals("Compartir 2 elementos", shareHeadline(2, 1, 1))
    }

    @Test
    fun featuredHidesMissingAppsAndKeepsMore() {
        val targets = featuredShareTargets(
            mimeTypes = listOf("image/jpeg"),
            installed = setOf(PACKAGE_INSTAGRAM, PACKAGE_WHATSAPP),
            nearbyLabel = "Quick Share",
            nearbyPackage = "com.google.android.gms",
            bluetoothLabel = "Bluetooth",
            bluetoothPackage = "com.android.bluetooth",
        )
        assertEquals(
            listOf(
                ShareTarget.ID_INSTAGRAM_STORY,
                ShareTarget.ID_INSTAGRAM,
                ShareTarget.ID_WHATSAPP,
                ShareTarget.ID_NEARBY,
                ShareTarget.ID_BLUETOOTH,
                ShareTarget.ID_MORE,
            ),
            targets.map { it.id },
        )
        assertTrue(targets.first { it.id == ShareTarget.ID_INSTAGRAM_STORY }.enabled)
        assertTrue(targets.all { it.enabled })
    }

    @Test
    fun storiesAndInstagramDisableWhenSeveralItemsAreSelected() {
        val targets = featuredShareTargets(
            mimeTypes = listOf("image/jpeg", "image/png"),
            installed = setOf(PACKAGE_INSTAGRAM, PACKAGE_FACEBOOK),
        )
        val story = targets.first { it.id == ShareTarget.ID_INSTAGRAM_STORY }
        val instagram = targets.first { it.id == ShareTarget.ID_INSTAGRAM }
        val facebookStory = targets.first { it.id == ShareTarget.ID_FACEBOOK_STORY }
        val facebook = targets.first { it.id == ShareTarget.ID_FACEBOOK }
        assertFalse(story.enabled)
        assertFalse(instagram.enabled)
        assertFalse(facebookStory.enabled)
        assertTrue(facebook.enabled)
        assertEquals("Elige una sola foto o video", story.reason)
        assertEquals("Instagram admite una sola foto o video", instagram.reason)
    }

    @Test
    fun facebookLiteFillsFacebookWithoutStories() {
        val targets = featuredShareTargets(
            mimeTypes = listOf("image/jpeg"),
            installed = setOf(PACKAGE_FACEBOOK_LITE),
        )
        assertNull(targets.firstOrNull { it.id == ShareTarget.ID_FACEBOOK_STORY })
        assertEquals(PACKAGE_FACEBOOK_LITE, targets.first { it.id == ShareTarget.ID_FACEBOOK }.packageName)
    }

    @Test
    fun mergeDropsFeaturedPackagesFromTheAppGrid() {
        val featured = featuredShareTargets(
            mimeTypes = listOf("image/jpeg"),
            installed = setOf(PACKAGE_INSTAGRAM),
        )
        val extras = listOf(
            ShareTarget(
                id = "app:$PACKAGE_INSTAGRAM",
                label = "Instagram",
                kind = ShareKind.APP,
                packageName = PACKAGE_INSTAGRAM,
            ),
            ShareTarget(
                id = "app:com.google.android.gm",
                label = "Gmail",
                kind = ShareKind.APP,
                packageName = "com.google.android.gm",
            ),
        )
        val merged = mergeShareTargets(featured, extras)
        assertTrue(merged.none { it.id == "app:$PACKAGE_INSTAGRAM" })
        assertTrue(merged.any { it.packageName == "com.google.android.gm" })
        assertEquals(ShareTarget.ID_MORE, merged.last { it.kind != ShareKind.APP }.id)
    }

    @Test
    fun nearbyAndBluetoothMatchKnownComponents() {
        assertTrue(
            isNearbyShareComponent(
                "com.google.android.gms",
                "com.google.android.gms.nearby.sharing.ShareSheetActivity",
            ),
        )
        assertTrue(
            isNearbyShareComponent(
                "com.samsung.android.app.sharelive",
                "com.samsung.android.app.sharelive.MainActivity",
            ),
        )
        assertTrue(
            isBluetoothShareComponent(
                "com.android.bluetooth",
                "com.android.bluetooth.opp.BluetoothOppLauncherActivity",
            ),
        )
        assertFalse(isNearbyShareComponent("com.whatsapp", "com.whatsapp.ContactPicker"))
        assertFalse(isBluetoothShareComponent("com.instagram.android", "com.instagram.share.ShareActivity"))
    }
}
