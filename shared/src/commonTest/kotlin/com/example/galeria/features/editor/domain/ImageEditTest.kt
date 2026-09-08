package com.example.galeria.features.editor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageEditTest {

    @Test
    fun squareCropFitsInsideLandscape() {
        val rect = cropRectForAspect(imageAspect = 16f / 9f, target = 1f)
        val imageWidth = 1600f
        val imageHeight = 900f
        val cropW = rect.width * imageWidth
        val cropH = rect.height * imageHeight
        assertTrue(kotlin.math.abs(cropW - cropH) < 2f)
        assertEquals(0f, rect.top)
        assertEquals(1f, rect.bottom)
    }

    @Test
    fun sixteenByNineCropFitsInsidePortrait() {
        val rect = cropRectForAspect(imageAspect = 9f / 16f, target = 16f / 9f)
        assertEquals(0f, rect.left)
        assertEquals(1f, rect.right)
        assertTrue(rect.height < 1f)
    }

    @Test
    fun freeEdgeDragKeepsTheRectangleAndTheOppositeSide() {
        val start = CropRect(0.1f, 0.2f, 0.9f, 0.8f)
        val next = start.dragged(
            handle = CropHandle.LEFT,
            dx = 0.15f,
            dy = 0.4f,
            targetPixelAspect = null,
            imageAspect = 16f / 9f,
        )
        assertEquals(0.25f, next.left, 0.001f)
        assertEquals(0.2f, next.top, 0.001f)
        assertEquals(0.9f, next.right, 0.001f)
        assertEquals(0.8f, next.bottom, 0.001f)
        assertTrue(CropQuad.fromRect(next).isAxisAligned())
    }

    @Test
    fun squareDragKeepsOneToOnePixels() {
        val start = cropRectForAspect(imageAspect = 16f / 9f, target = 1f)
        val next = start.dragged(
            handle = CropHandle.RIGHT,
            dx = -0.08f,
            dy = 0f,
            targetPixelAspect = 1f,
            imageAspect = 16f / 9f,
        )
        val cropW = next.width * 16f
        val cropH = next.height * 9f
        assertTrue(kotlin.math.abs(cropW - cropH) < 0.05f)
        assertTrue(CropQuad.fromRect(next).isAxisAligned())
    }
}
