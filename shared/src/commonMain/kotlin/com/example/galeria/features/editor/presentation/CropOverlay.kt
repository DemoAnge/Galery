package com.example.galeria.features.editor.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.galeria.features.editor.domain.CropAspect
import com.example.galeria.features.editor.domain.CropHandle
import com.example.galeria.features.editor.domain.CropQuad
import com.example.galeria.features.editor.domain.CropRect
import com.example.galeria.features.editor.domain.dragged
import com.example.galeria.features.editor.domain.pixelRatio
import kotlin.math.abs
import kotlin.math.hypot

@Composable
internal fun CropOverlay(
    imageWidth: Int,
    imageHeight: Int,
    crop: CropQuad,
    aspect: CropAspect,
    enabled: Boolean,
    onCropChange: (CropQuad) -> Unit,
    modifier: Modifier = Modifier,
) {
    var container by remember { mutableStateOf(IntSize.Zero) }
    var latest by remember { mutableStateOf(crop.boundingRect()) }
    latest = crop.boundingRect()
    val imageRect = remember(container, imageWidth, imageHeight) {
        fittedImageRect(container.width.toFloat(), container.height.toFloat(), imageWidth, imageHeight)
    }
    val lockedRatio = aspect.pixelRatio(imageWidth, imageHeight)
    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { container = it }
            .pointerInput(enabled, imageRect, aspect, imageWidth, imageHeight) {
                if (!enabled || imageRect.width <= 1f) return@pointerInput
                var active: CropHandle? = null
                detectDragGestures(
                    onDragStart = { start ->
                        active = hitHandle(start, imageRect, latest)
                    },
                    onDragEnd = { active = null },
                    onDragCancel = { active = null },
                    onDrag = { change, drag ->
                        change.consume()
                        val handle = active ?: return@detectDragGestures
                        val dx = if (imageRect.width == 0f) 0f else drag.x / imageRect.width
                        val dy = if (imageRect.height == 0f) 0f else drag.y / imageRect.height
                        val imageAspect = imageRect.width / imageRect.height.coerceAtLeast(1f)
                        val next = latest.dragged(handle, dx, dy, lockedRatio, imageAspect)
                        latest = next
                        onCropChange(CropQuad.fromRect(next))
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (imageRect.width <= 1f) return@Canvas
            val box = crop.boundingRect().toDisplay(imageRect)
            val dim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addRect(box)
            }
            drawPath(dim, Color.Black.copy(alpha = 0.55f))
            drawRect(Color.White, topLeft = box.topLeft, size = box.size, style = Stroke(width = 2.dp.toPx()))
            val v1 = box.left + box.width / 3f
            val v2 = box.left + box.width * 2f / 3f
            val h1 = box.top + box.height / 3f
            val h2 = box.top + box.height * 2f / 3f
            drawLine(Color.White.copy(alpha = 0.4f), Offset(v1, box.top), Offset(v1, box.bottom), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.4f), Offset(v2, box.top), Offset(v2, box.bottom), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.4f), Offset(box.left, h1), Offset(box.right, h1), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.4f), Offset(box.left, h2), Offset(box.right, h2), 1.dp.toPx())
            val corner = 9.dp.toPx()
            listOf(box.topLeft, box.topRight, box.bottomRight, box.bottomLeft).forEach { point ->
                drawCircle(Color.White, radius = corner, center = point)
            }
            val bar = 28.dp.toPx()
            val thick = 5.dp.toPx()
            val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            drawRoundRect(
                Color.White,
                topLeft = Offset(box.center.x - bar / 2f, box.top - thick / 2f),
                size = Size(bar, thick),
                cornerRadius = radius,
            )
            drawRoundRect(
                Color.White,
                topLeft = Offset(box.center.x - bar / 2f, box.bottom - thick / 2f),
                size = Size(bar, thick),
                cornerRadius = radius,
            )
            drawRoundRect(
                Color.White,
                topLeft = Offset(box.left - thick / 2f, box.center.y - bar / 2f),
                size = Size(thick, bar),
                cornerRadius = radius,
            )
            drawRoundRect(
                Color.White,
                topLeft = Offset(box.right - thick / 2f, box.center.y - bar / 2f),
                size = Size(thick, bar),
                cornerRadius = radius,
            )
        }
    }
}

private fun CropRect.toDisplay(imageRect: Rect): Rect = Rect(
    left = imageRect.left + left * imageRect.width,
    top = imageRect.top + top * imageRect.height,
    right = imageRect.left + right * imageRect.width,
    bottom = imageRect.top + bottom * imageRect.height,
)

private fun fittedImageRect(containerW: Float, containerH: Float, imageW: Int, imageH: Int): Rect {
    if (containerW <= 0f || containerH <= 0f || imageW <= 0 || imageH <= 0) {
        return Rect(0f, 0f, 0f, 0f)
    }
    val cAspect = containerW / containerH
    val iAspect = imageW.toFloat() / imageH.toFloat()
    return if (iAspect > cAspect) {
        val height = containerW / iAspect
        val top = (containerH - height) / 2f
        Rect(0f, top, containerW, top + height)
    } else {
        val width = containerH * iAspect
        val left = (containerW - width) / 2f
        Rect(left, 0f, left + width, containerH)
    }
}

private fun hitHandle(point: Offset, imageRect: Rect, crop: CropRect): CropHandle {
    val box = crop.toDisplay(imageRect)
    val slop = 28f
    val corners = listOf(
        CropHandle.TL to box.topLeft,
        CropHandle.TR to box.topRight,
        CropHandle.BR to box.bottomRight,
        CropHandle.BL to box.bottomLeft,
    )
    corners.forEach { (handle, corner) ->
        if (hypot(point.x - corner.x, point.y - corner.y) <= slop) return handle
    }
    if (abs(point.y - box.top) <= slop && point.x in box.left..box.right) return CropHandle.TOP
    if (abs(point.y - box.bottom) <= slop && point.x in box.left..box.right) return CropHandle.BOTTOM
    if (abs(point.x - box.left) <= slop && point.y in box.top..box.bottom) return CropHandle.LEFT
    if (abs(point.x - box.right) <= slop && point.y in box.top..box.bottom) return CropHandle.RIGHT
    return CropHandle.MOVE
}
