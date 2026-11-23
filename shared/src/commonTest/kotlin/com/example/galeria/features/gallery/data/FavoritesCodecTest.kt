package com.example.galeria.features.gallery.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoritesCodecTest {

    @Test
    fun roundTripKeepsIdsAndDropsBlanks() {
        val source = setOf("content://a", "content://b")
        val restored = FavoritesCodec.decode(FavoritesCodec.encode(source))
        assertEquals(source, restored)
        assertTrue(FavoritesCodec.decode(" \n \ncontent://a\n").contains("content://a"))
        assertTrue(FavoritesCodec.decode("").isEmpty())
    }
}
