package com.example.galeria.features.vault.presentation

import com.example.galeria.features.vault.data.canBackfillVaultThumbnail
import com.example.galeria.features.vault.data.looksLikeStillImage
import com.example.galeria.features.vault.data.vaultThumbnailFileId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VaultThumbnailTest {

    @Test
    fun thumbnailIdStaysAValidSidecarName() {
        assertEquals("ab12.thumb", vaultThumbnailFileId("ab12"))
    }

    @Test
    fun stillImageMagicIsDetectedWithoutDecodingTheFile() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) + ByteArray(12)
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) + ByteArray(12)
        val mp4 = byteArrayOf(0, 0, 0, 0x18, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D)
        assertTrue(looksLikeStillImage(jpeg))
        assertTrue(looksLikeStillImage(png))
        assertFalse(looksLikeStillImage(mp4))
        assertFalse(looksLikeStillImage(ByteArray(4)))
    }

    @Test
    fun largeVideosAreNotFullyDecryptedJustToBuildAThumb() {
        assertTrue(canBackfillVaultThumbnail(2L * 1024 * 1024))
        assertFalse(canBackfillVaultThumbnail(80L * 1024 * 1024))
        assertFalse(canBackfillVaultThumbnail(0L))
    }
}
