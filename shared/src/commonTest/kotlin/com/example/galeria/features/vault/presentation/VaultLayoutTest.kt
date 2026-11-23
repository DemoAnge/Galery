package com.example.galeria.features.vault.presentation

import com.example.galeria.features.vault.domain.VaultItem
import com.example.galeria.features.vault.domain.VaultItemId
import com.example.galeria.features.vault.domain.VaultMediaKind
import com.example.galeria.features.vault.domain.LockMethod
import com.example.galeria.features.vault.domain.allowsPinRecovery
import com.example.galeria.features.vault.domain.requirePin
import com.example.galeria.features.vault.domain.lockoutDelayMs
import com.example.galeria.features.vault.domain.lockoutRemainingMs
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
    fun pinMustBeSixDigitsToChangeTheLock() {
        requirePin(CharArray(LockMethod.PIN_DIGITS) { '1' })
        assertEquals(LockMethod.PIN_DIGITS, LockMethod.PIN_MIN_DIGITS)
    }

    @Test
    fun lockoutStartsAfterFiveFailures() {
        assertEquals(0L, lockoutDelayMs(4))
        assertEquals(30_000L, lockoutDelayMs(5))
        assertEquals(1_800_000L, lockoutDelayMs(12))
        assertEquals(20_000L, lockoutRemainingMs(5, lastFailureEpochMs = 1_000L, nowEpochMs = 11_000L))
        assertEquals(0L, lockoutRemainingMs(5, lastFailureEpochMs = 1_000L, nowEpochMs = 40_000L))
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
