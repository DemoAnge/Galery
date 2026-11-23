package com.example.galeria.features.share.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class ShareMimeTypeTest {

    @Test
    fun oneTypeKeepsTheSpecificMimeAndMixedUsesWildcard() {
        assertEquals("image/jpeg", sharedMimeType(listOf("image/jpeg", "image/jpeg")))
        assertEquals("*/*", sharedMimeType(listOf("image/jpeg", "video/mp4")))
    }
}
