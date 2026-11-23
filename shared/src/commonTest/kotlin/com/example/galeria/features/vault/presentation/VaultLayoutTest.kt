package com.example.galeria.features.vault.presentation

import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultItemId
import com.example.galeria.features.vault.domain.VaultMediaKind
import com.example.galeria.features.vault.domain.allowsPinRecovery
import com.example.galeria.features.vault.domain.shouldPurgeOrphanSecrets
import com.example.galeria.features.vault.domain.shouldResetLockWhenLeaving
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultLayoutTest {

    @Test
    fun photosAndVideosOnlyAppearWhenPresent() {
        val photos = listOf(item("p1", VaultMediaKind.IMAGE), item("p2", VaultMediaKind.IMAGE))
        val videos = listOf(item("v1", VaultMediaKind.VIDEO))
        assertEquals(2, vaultPhotos(photos + videos).size)
        assertEquals(1, vaultVideos(photos + videos).size)
        assertTrue(vaultPhotos(videos).isEmpty())
        assertTrue(vaultVideos(photos).isEmpty())
    }

    @Test
    fun collectionsStayHiddenUntilItemsAreGrouped() {
        val lone = listOf(item("p1", VaultMediaKind.IMAGE, album = "Cámara"))
        assertTrue(vaultCollections(lone).isEmpty())

        val grouped = listOf(
            item("p1", VaultMediaKind.IMAGE, album = "Cámara"),
            item("p2", VaultMediaKind.IMAGE, album = "Cámara"),
            item("w1", VaultMediaKind.IMAGE, album = "WhatsApp"),
        )
        val collections = vaultCollections(grouped)
        assertEquals(2, collections.size)
        assertEquals("Cámara", collections.first().title)
        assertEquals(2, collections.first().items.size)
    }

    @Test
    fun emptyVaultAsksForANewPinOnTheNextVisit() {
        assertTrue(shouldResetLockWhenLeaving(0))
        assertFalse(shouldResetLockWhenLeaving(1))
        assertFalse(shouldResetLockWhenLeaving(4))
    }

    @Test
    fun forgottenPinCannotBeRecovered() {
        assertFalse(allowsPinRecovery())
    }

    @Test
    fun missingSandboxMarkerPurgesLeftoverSecrets() {
        assertTrue(shouldPurgeOrphanSecrets(hasInstallMarker = false))
        assertFalse(shouldPurgeOrphanSecrets(hasInstallMarker = true))
    }

    @Test
    fun unlabeledItemsDoNotCreateCollections() {
        val mixed = listOf(
            item("p1", VaultMediaKind.IMAGE),
            item("p2", VaultMediaKind.IMAGE),
        )
        assertTrue(vaultCollections(mixed).isEmpty())
    }

    private fun item(
        id: String,
        kind: VaultMediaKind,
        album: String? = null,
    ): VaultItem = VaultItem(
        id = VaultItemId(id),
        displayName = "$id.jpg",
        mediaKind = kind,
        capturedAtEpochMs = 1_700_000_000_000L,
        sizeBytes = 10L,
        addedAtEpochMs = 1_700_000_000_000L,
        albumLabel = album,
    )
}
