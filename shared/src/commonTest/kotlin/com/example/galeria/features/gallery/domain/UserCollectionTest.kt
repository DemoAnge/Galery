package com.example.galeria.features.gallery.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserCollectionTest {

    @Test
    fun newAlbumKeepsDistinctPhotoIds() {
        val album = newUserCollection(emptyList(), "Viaje", listOf("a", "a", "b"))
        assertEquals("Viaje", album.title)
        assertEquals(listOf("a", "b"), album.itemIds)
        assertTrue(album.id.startsWith("c-"))
    }

    @Test
    fun movingAddsPhotosToTheTargetAlbum() {
        val trip = UserCollection("c-1", "Viaje", listOf("a"))
        val family = UserCollection("c-2", "Familia", emptyList())
        val next = listOf(trip, family).movingTo("c-2", listOf("b", "c"))
        assertEquals(listOf("a"), next.first { it.id == "c-1" }.itemIds)
        assertEquals(listOf("b", "c"), next.first { it.id == "c-2" }.itemIds)
    }

    @Test
    fun movingFromAnotherAlbumRemovesThemFromTheSource() {
        val trip = UserCollection("c-1", "Viaje", listOf("a", "b"))
        val family = UserCollection("c-2", "Familia", listOf("c"))
        val next = listOf(trip, family).movingTo("c-2", listOf("b"), sourceCustomId = "c-1")
        assertEquals(listOf("a"), next.first { it.id == "c-1" }.itemIds)
        assertEquals(listOf("c", "b"), next.first { it.id == "c-2" }.itemIds)
    }

    @Test
    fun renamingChangesOnlyThatAlbumTitle() {
        val trip = UserCollection("c-1", "Viaje", listOf("a"))
        val family = UserCollection("c-2", "Familia", listOf("b"))
        val next = listOf(trip, family).renaming("c-1", "  Costa  ")
        assertEquals("Costa", next.first { it.id == "c-1" }.title)
        assertEquals("Familia", next.first { it.id == "c-2" }.title)
        assertEquals(listOf(trip, family), listOf(trip, family).renaming("c-1", "  "))
    }

    @Test
    fun removingFromAnAlbumLeavesTheFilesInOtherAlbums() {
        val trip = UserCollection("c-1", "Viaje", listOf("a", "b", "c"))
        val family = UserCollection("c-2", "Familia", listOf("b"))
        val next = listOf(trip, family).removingFrom("c-1", listOf("b", "c"))
        assertEquals(listOf("a"), next.first { it.id == "c-1" }.itemIds)
        assertEquals(listOf("b"), next.first { it.id == "c-2" }.itemIds)
    }

    @Test
    fun deletingAnAlbumKeepsTheOthers() {
        val trip = UserCollection("c-1", "Viaje", listOf("a"))
        val family = UserCollection("c-2", "Familia", listOf("b"))
        val next = listOf(trip, family).without("c-1")
        assertEquals(listOf(family), next)
    }

    @Test
    fun movingIntoTheSameAlbumDoesNotDropPhotos() {
        val trip = UserCollection("c-1", "Viaje", listOf("a"))
        val next = listOf(trip).movingTo("c-1", listOf("b"), sourceCustomId = "c-1")
        assertEquals(listOf("a", "b"), next.single().itemIds)
    }
}
