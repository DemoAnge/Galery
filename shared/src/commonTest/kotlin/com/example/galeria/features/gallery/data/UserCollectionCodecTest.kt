package com.example.galeria.features.gallery.data

import com.example.galeria.features.gallery.domain.UserCollection
import kotlin.test.Test
import kotlin.test.assertEquals

class UserCollectionCodecTest {

    @Test
    fun roundTripKeepsAlbumAndPhotoIds() {
        val source = listOf(
            UserCollection("c-1", "Viaje", listOf("content://a", "content://b")),
            UserCollection("c-2", "Familia", emptyList()),
        )
        val restored = UserCollectionCodec.decode(UserCollectionCodec.encode(source))
        assertEquals(source, restored)
    }
}
