package com.example.galeria.features.editor.domain

import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.ColorMatrix
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader
import org.jetbrains.skia.Surface
import kotlin.math.max

actual class ImageEditor actual constructor() {

    actual fun apply(input: ByteArray, edit: ImageEdit, maxEdge: Int): ByteArray {
        var image = Image.makeFromEncoded(input)
        image = fitMaxEdge(image, maxEdge)
        image = flip(image, edit.flipHorizontal, edit.flipVertical)
        image = rotate(image, edit.rotationDegrees.toFloat() + edit.straighten)
        image = crop(image, edit)
        image = color(image, edit)
        image = vignette(image, edit.vignette)
        val encoded = image.encodeToData(EncodedImageFormat.JPEG, 95)
            ?: error("Image encode failed")
        return encoded.bytes
    }

    private fun fitMaxEdge(source: Image, maxEdge: Int): Image {
        if (maxEdge <= 0) return source
        val largest = max(source.width, source.height)
        if (largest <= maxEdge) return source
        val scale = maxEdge.toFloat() / largest
        val width = (source.width * scale).toInt().coerceAtLeast(1)
        val height = (source.height * scale).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(width, height)
        surface.canvas.drawImageRect(
            source,
            Rect.makeWH(source.width.toFloat(), source.height.toFloat()),
            Rect.makeWH(width.toFloat(), height.toFloat()),
        )
        return surface.makeImageSnapshot()
    }

    private fun flip(source: Image, horizontal: Boolean, vertical: Boolean): Image {
        if (!horizontal && !vertical) return source
        val surface = Surface.makeRasterN32Premul(source.width, source.height)
        val canvas = surface.canvas
        val sx = if (horizontal) -1f else 1f
        val sy = if (vertical) -1f else 1f
        canvas.translate(source.width / 2f, source.height / 2f)
        canvas.scale(sx, sy)
        canvas.translate(-source.width / 2f, -source.height / 2f)
        canvas.drawImage(source, 0f, 0f)
        return surface.makeImageSnapshot()
    }

    private fun rotate(source: Image, degrees: Float): Image {
        val normalized = ((degrees % 360f) + 360f) % 360f
        if (normalized < 0.05f || normalized > 359.95f) return source
        val radians = Math.toRadians(normalized.toDouble())
        val cos = kotlin.math.abs(kotlin.math.cos(radians)).toFloat()
        val sin = kotlin.math.abs(kotlin.math.sin(radians)).toFloat()
        val outW = (source.width * cos + source.height * sin).toInt().coerceAtLeast(1)
        val outH = (source.width * sin + source.height * cos).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(outW, outH)
        val canvas = surface.canvas
        canvas.translate(outW / 2f, outH / 2f)
        canvas.rotate(normalized)
        canvas.translate(-source.width / 2f, -source.height / 2f)
        canvas.drawImage(source, 0f, 0f)
        return surface.makeImageSnapshot()
    }

    private fun crop(source: Image, edit: ImageEdit): Image {
        if (edit.isIdentityCrop()) return source
        val quad = edit.resolvedCrop()
        val w = source.width.toFloat()
        val h = source.height.toFloat()
        if (quad.isAxisAligned()) {
            val rect = quad.boundingRect()
            val x = rect.left * w
            val y = rect.top * h
            val width = (rect.width * w).coerceAtLeast(1f)
            val height = (rect.height * h).coerceAtLeast(1f)
            val surface = Surface.makeRasterN32Premul(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1))
            surface.canvas.drawImageRect(
                source,
                Rect.makeXYWH(x, y, width, height),
                Rect.makeWH(width, height),
            )
            return surface.makeImageSnapshot()
        }
        val outW = ((dist(quad.tlX * w, quad.tlY * h, quad.trX * w, quad.trY * h) +
            dist(quad.blX * w, quad.blY * h, quad.brX * w, quad.brY * h)) / 2f).toInt().coerceAtLeast(1)
        val outH = ((dist(quad.tlX * w, quad.tlY * h, quad.blX * w, quad.blY * h) +
            dist(quad.trX * w, quad.trY * h, quad.brX * w, quad.brY * h)) / 2f).toInt().coerceAtLeast(1)
        val src = floatArrayOf(
            quad.tlX * w, quad.tlY * h,
            quad.trX * w, quad.trY * h,
            quad.brX * w, quad.brY * h,
            quad.blX * w, quad.blY * h,
        )
        val dst = floatArrayOf(0f, 0f, outW.toFloat(), 0f, outW.toFloat(), outH.toFloat(), 0f, outH.toFloat())
        val matrix = polyToPoly(src, dst)
        val surface = Surface.makeRasterN32Premul(outW, outH)
        val canvas = surface.canvas
        if (matrix != null) {
            canvas.concat(org.jetbrains.skia.Matrix33.makeAll(
                matrix[0], matrix[1], matrix[2],
                matrix[3], matrix[4], matrix[5],
                matrix[6], matrix[7], matrix[8],
            ))
            canvas.drawImage(source, 0f, 0f)
        } else {
            val rect = quad.boundingRect()
            canvas.drawImageRect(
                source,
                Rect.makeXYWH(rect.left * w, rect.top * h, rect.width * w, rect.height * h),
                Rect.makeWH(outW.toFloat(), outH.toFloat()),
            )
        }
        return surface.makeImageSnapshot()
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun color(source: Image, edit: ImageEdit): Image {
        val surface = Surface.makeRasterN32Premul(source.width, source.height)
        val paint = Paint()
        paint.colorFilter = ColorFilter.makeMatrix(ColorMatrix(*editorColorMatrix(edit)))
        surface.canvas.drawImage(source, 0f, 0f, paint)
        return surface.makeImageSnapshot()
    }

    private fun vignette(source: Image, amount: Float): Image {
        if (amount <= 0.01f) return source
        val surface = Surface.makeRasterN32Premul(source.width, source.height)
        val canvas = surface.canvas
        canvas.drawImage(source, 0f, 0f)
        val cx = source.width / 2f
        val cy = source.height / 2f
        val radius = max(source.width, source.height) * 0.72f
        val alpha = (amount.coerceIn(0f, 1f) * 180f).toInt() shl 24
        val paint = Paint()
        paint.shader = Shader.makeRadialGradient(
            Point(cx, cy),
            radius,
            intArrayOf(0x00000000, alpha),
            floatArrayOf(0.42f, 1f),
            GradientStyle(FilterTileMode.CLAMP, false, null),
        )
        canvas.drawRect(Rect.makeWH(source.width.toFloat(), source.height.toFloat()), paint)
        return surface.makeImageSnapshot()
    }
}
