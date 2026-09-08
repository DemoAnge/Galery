package com.example.galeria.features.editor.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

actual class ImageEditor actual constructor() {

    actual fun apply(input: ByteArray, edit: ImageEdit, maxEdge: Int): ByteArray {
        var bitmap = decode(input, maxEdge)
        bitmap = flip(bitmap, edit.flipHorizontal, edit.flipVertical)
        bitmap = rotate(bitmap, edit.rotationDegrees.toFloat() + edit.straighten)
        bitmap = crop(bitmap, edit)
        bitmap = color(bitmap, edit)
        bitmap = vignette(bitmap, edit.vignette)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        return out.toByteArray()
    }

    private fun decode(input: ByteArray, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        var sample = 1
        val largest = max(bounds.outWidth, bounds.outHeight)
        if (maxEdge > 0) {
            while (largest / sample > maxEdge) sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(input, 0, input.size, options)
            ?: error("Image decode failed")
    }

    private fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        if (!horizontal && !vertical) return bitmap
        val matrix = Matrix().apply {
            postScale(if (horizontal) -1f else 1f, if (vertical) -1f else 1f, bitmap.width / 2f, bitmap.height / 2f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val normalized = ((degrees % 360f) + 360f) % 360f
        if (normalized < 0.05f || normalized > 359.95f) return bitmap
        val matrix = Matrix().apply { postRotate(normalized) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun crop(bitmap: Bitmap, edit: ImageEdit): Bitmap {
        if (edit.isIdentityCrop()) return bitmap
        val quad = edit.resolvedCrop()
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()
        if (quad.isAxisAligned()) {
            val rect = quad.boundingRect()
            val x = (rect.left * w).toInt().coerceIn(0, bitmap.width - 1)
            val y = (rect.top * h).toInt().coerceIn(0, bitmap.height - 1)
            val width = (rect.width * w).toInt().coerceAtLeast(1).coerceAtMost(bitmap.width - x)
            val height = (rect.height * h).toInt().coerceAtLeast(1).coerceAtMost(bitmap.height - y)
            return Bitmap.createBitmap(bitmap, x, y, width, height)
        }
        val src = floatArrayOf(
            quad.tlX * w, quad.tlY * h,
            quad.trX * w, quad.trY * h,
            quad.brX * w, quad.brY * h,
            quad.blX * w, quad.blY * h,
        )
        val outW = ((dist(src[0], src[1], src[2], src[3]) + dist(src[6], src[7], src[4], src[5])) / 2f)
            .toInt().coerceAtLeast(1)
        val outH = ((dist(src[0], src[1], src[6], src[7]) + dist(src[2], src[3], src[4], src[5])) / 2f)
            .toInt().coerceAtLeast(1)
        val dst = floatArrayOf(0f, 0f, outW.toFloat(), 0f, outW.toFloat(), outH.toFloat(), 0f, outH.toFloat())
        val matrix = Matrix()
        if (!matrix.setPolyToPoly(src, 0, dst, 0, 4)) {
            val rect = quad.boundingRect()
            val x = (rect.left * w).toInt().coerceIn(0, bitmap.width - 1)
            val y = (rect.top * h).toInt().coerceIn(0, bitmap.height - 1)
            val width = (rect.width * w).toInt().coerceAtLeast(1).coerceAtMost(bitmap.width - x)
            val height = (rect.height * h).toInt().coerceAtLeast(1).coerceAtMost(bitmap.height - y)
            return Bitmap.createBitmap(bitmap, x, y, width, height)
        }
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun color(bitmap: Bitmap, edit: ImageEdit): Bitmap {
        val out = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix(editorColorMatrix(edit)))
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return out
    }

    private fun vignette(bitmap: Bitmap, amount: Float): Bitmap {
        if (amount <= 0.01f) return bitmap
        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val cx = bitmap.width / 2f
        val cy = bitmap.height / 2f
        val radius = max(bitmap.width, bitmap.height) * 0.72f
        val alpha = (min(1f, amount) * 180f).toInt()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0)),
                floatArrayOf(0.42f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        return out
    }
}
