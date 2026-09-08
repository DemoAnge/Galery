package com.example.galeria.features.editor.domain

import kotlin.math.abs

enum class ImageFilter {
    NONE,
    VIVID,
    PORTRAIT,
    CINEMA,
    COOL,
    WARM,
    DRAMATIC,
    GRAYSCALE,
    FILM,
    FADE,
}

enum class CropAspect {
    FREE,
    ORIGINAL,
    SQUARE,
    RATIO_3_4,
    RATIO_4_3,
    RATIO_9_16,
    RATIO_16_9,
    RATIO_2_3,
    RATIO_3_2,
}

data class ImageEdit(
    val rotationDegrees: Int = 0,
    val straighten: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropAspect: CropAspect = CropAspect.FREE,
    val crop: CropQuad = CropQuad(),
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val filter: ImageFilter = ImageFilter.NONE,
    val autoPerfect: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val vignette: Float = 0f,
)

fun ImageEdit.withoutCrop(): ImageEdit = copy(
    crop = CropQuad(),
    cropLeft = 0f,
    cropTop = 0f,
    cropRight = 1f,
    cropBottom = 1f,
)

fun ImageEdit.resolvedCrop(): CropQuad {
    val fromCorners = crop.normalized()
    if (!fromCorners.isIdentity()) return fromCorners
    return CropQuad.fromRect(CropRect(cropLeft, cropTop, cropRight, cropBottom).clamped())
}

fun ImageEdit.isIdentityCrop(): Boolean = resolvedCrop().isIdentity()

fun CropAspect.label(): String = when (this) {
    CropAspect.FREE -> "Libre"
    CropAspect.ORIGINAL -> "Original"
    CropAspect.SQUARE -> "1:1"
    CropAspect.RATIO_3_4 -> "3:4"
    CropAspect.RATIO_4_3 -> "4:3"
    CropAspect.RATIO_9_16 -> "9:16"
    CropAspect.RATIO_16_9 -> "16:9"
    CropAspect.RATIO_2_3 -> "2:3"
    CropAspect.RATIO_3_2 -> "3:2"
}

fun CropAspect.pixelRatio(imageWidth: Int, imageHeight: Int): Float? {
    if (imageWidth <= 0 || imageHeight <= 0) return null
    val original = imageWidth.toFloat() / imageHeight.toFloat()
    return when (this) {
        CropAspect.FREE -> null
        CropAspect.ORIGINAL -> original
        CropAspect.SQUARE -> 1f
        CropAspect.RATIO_3_4 -> 3f / 4f
        CropAspect.RATIO_4_3 -> 4f / 3f
        CropAspect.RATIO_9_16 -> 9f / 16f
        CropAspect.RATIO_16_9 -> 16f / 9f
        CropAspect.RATIO_2_3 -> 2f / 3f
        CropAspect.RATIO_3_2 -> 3f / 2f
    }
}

fun cropRectForAspect(imageAspect: Float, target: Float): CropRect {
    val safeImage = imageAspect.coerceAtLeast(0.01f)
    val safeTarget = target.coerceAtLeast(0.01f)
    return if (safeImage > safeTarget) {
        val width = (safeTarget / safeImage).coerceIn(0.12f, 1f)
        val left = (1f - width) / 2f
        CropRect(left, 0f, left + width, 1f)
    } else {
        val height = (safeImage / safeTarget).coerceIn(0.12f, 1f)
        val top = (1f - height) / 2f
        CropRect(0f, top, 1f, top + height)
    }
}

data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0.01f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.01f)

    fun clamped(): CropRect {
        val l = left.coerceIn(0f, 0.88f)
        val t = top.coerceIn(0f, 0.88f)
        val r = right.coerceIn(l + 0.12f, 1f)
        val b = bottom.coerceIn(t + 0.12f, 1f)
        return CropRect(l, t, r, b)
    }
}

enum class CropHandle { TL, TR, BR, BL, LEFT, RIGHT, TOP, BOTTOM, MOVE }

fun CropRect.dragged(
    handle: CropHandle,
    dx: Float,
    dy: Float,
    targetPixelAspect: Float?,
    imageAspect: Float,
): CropRect {
    if (handle == CropHandle.MOVE) {
        val w = width
        val h = height
        val left = (left + dx).coerceIn(0f, 1f - w)
        val top = (top + dy).coerceIn(0f, 1f - h)
        return CropRect(left, top, left + w, top + h)
    }
    var nextLeft = left
    var nextTop = top
    var nextRight = right
    var nextBottom = bottom
    when (handle) {
        CropHandle.LEFT, CropHandle.TL, CropHandle.BL -> nextLeft += dx
        CropHandle.RIGHT, CropHandle.TR, CropHandle.BR -> nextRight += dx
        else -> Unit
    }
    when (handle) {
        CropHandle.TOP, CropHandle.TL, CropHandle.TR -> nextTop += dy
        CropHandle.BOTTOM, CropHandle.BL, CropHandle.BR -> nextBottom += dy
        else -> Unit
    }
    var next = CropRect(nextLeft, nextTop, nextRight, nextBottom).clamped()
    if (targetPixelAspect != null) {
        next = next.withLockedAspect(targetPixelAspect, imageAspect, handle)
    }
    return next
}

internal fun CropRect.withLockedAspect(
    targetPixelAspect: Float,
    imageAspect: Float,
    handle: CropHandle,
): CropRect {
    val targetUv = targetPixelAspect / imageAspect.coerceAtLeast(0.01f)
    return when (handle) {
        CropHandle.MOVE -> this
        CropHandle.LEFT, CropHandle.RIGHT -> {
            val nextHeight = (width / targetUv).coerceIn(0.12f, 1f)
            val centerY = (top + bottom) / 2f
            var nextTop = (centerY - nextHeight / 2f).coerceAtLeast(0f)
            if (nextTop + nextHeight > 1f) nextTop = 1f - nextHeight
            CropRect(left, nextTop, right, nextTop + nextHeight).clamped()
        }
        CropHandle.TOP, CropHandle.BOTTOM -> {
            val nextWidth = (height * targetUv).coerceIn(0.12f, 1f)
            val centerX = (left + right) / 2f
            var nextLeft = (centerX - nextWidth / 2f).coerceAtLeast(0f)
            if (nextLeft + nextWidth > 1f) nextLeft = 1f - nextWidth
            CropRect(nextLeft, top, nextLeft + nextWidth, bottom).clamped()
        }
        CropHandle.TL, CropHandle.TR -> {
            val nextHeight = (width / targetUv).coerceIn(0.12f, 1f)
            val nextTop = (bottom - nextHeight).coerceAtLeast(0f)
            CropRect(left, nextTop, right, bottom).clamped()
        }
        CropHandle.BL, CropHandle.BR -> {
            val nextHeight = (width / targetUv).coerceIn(0.12f, 1f)
            val nextTop = top.coerceAtMost(1f - nextHeight)
            CropRect(left, nextTop, right, nextTop + nextHeight).clamped()
        }
    }
}

data class CropQuad(
    val tlX: Float = 0f,
    val tlY: Float = 0f,
    val trX: Float = 1f,
    val trY: Float = 0f,
    val brX: Float = 1f,
    val brY: Float = 1f,
    val blX: Float = 0f,
    val blY: Float = 1f,
) {
    fun normalized(): CropQuad = CropQuad(
        tlX.coerceIn(0f, 1f),
        tlY.coerceIn(0f, 1f),
        trX.coerceIn(0f, 1f),
        trY.coerceIn(0f, 1f),
        brX.coerceIn(0f, 1f),
        brY.coerceIn(0f, 1f),
        blX.coerceIn(0f, 1f),
        blY.coerceIn(0f, 1f),
    )

    fun isIdentity(): Boolean =
        tlX <= 0.001f && tlY <= 0.001f &&
            trX >= 0.999f && trY <= 0.001f &&
            brX >= 0.999f && brY >= 0.999f &&
            blX <= 0.001f && blY >= 0.999f

    fun isAxisAligned(): Boolean {
        val eps = 0.012f
        return abs(tlY - trY) <= eps &&
            abs(blY - brY) <= eps &&
            abs(tlX - blX) <= eps &&
            abs(trX - brX) <= eps
    }

    fun boundingRect(): CropRect = CropRect(
        left = minOf(tlX, trX, brX, blX),
        top = minOf(tlY, trY, brY, blY),
        right = maxOf(tlX, trX, brX, blX),
        bottom = maxOf(tlY, trY, brY, blY),
    ).clamped()

    companion object {
        fun fromRect(rect: CropRect): CropQuad {
            val r = rect.clamped()
            return CropQuad(r.left, r.top, r.right, r.top, r.right, r.bottom, r.left, r.bottom)
        }
    }
}

fun ImageEdit.cropRect(): CropRect = resolvedCrop().boundingRect()

fun ImageEdit.withCrop(rect: CropRect): ImageEdit {
    val next = CropQuad.fromRect(rect)
    return copy(
        crop = next,
        cropLeft = next.tlX,
        cropTop = next.tlY,
        cropRight = next.trX,
        cropBottom = next.brY,
    )
}

fun ImageEdit.withQuad(quad: CropQuad): ImageEdit {
    val next = quad.normalized()
    val bounds = next.boundingRect()
    return copy(
        crop = next,
        cropLeft = bounds.left,
        cropTop = bounds.top,
        cropRight = bounds.right,
        cropBottom = bounds.bottom,
    )
}

internal fun editorColorMatrix(edit: ImageEdit): FloatArray {
    var saturation = edit.saturation
    var contrast = edit.contrast
    var brightness = edit.brightness
    var warmth = edit.warmth
    when (edit.filter) {
        ImageFilter.NONE -> Unit
        ImageFilter.VIVID -> {
            saturation *= 1.32f
            contrast *= 1.08f
        }
        ImageFilter.PORTRAIT -> {
            warmth += 0.08f
            contrast *= 0.96f
            saturation *= 1.06f
        }
        ImageFilter.CINEMA -> {
            saturation *= 0.88f
            contrast *= 1.18f
            warmth -= 0.04f
        }
        ImageFilter.COOL -> warmth -= 0.18f
        ImageFilter.WARM -> warmth += 0.2f
        ImageFilter.DRAMATIC -> {
            contrast *= 1.28f
            saturation *= 1.12f
            brightness -= 0.04f
        }
        ImageFilter.GRAYSCALE -> saturation = 0f
        ImageFilter.FILM -> {
            contrast *= 0.9f
            brightness += 0.06f
            warmth += 0.1f
            saturation *= 0.92f
        }
        ImageFilter.FADE -> {
            contrast *= 0.82f
            brightness += 0.1f
            saturation *= 0.86f
        }
    }
    if (edit.autoPerfect) {
        brightness += 0.05f
        contrast *= 1.1f
        saturation *= 1.08f
        warmth += 0.04f
    }
    brightness += edit.shadows * 0.22f
    contrast += edit.highlights * 0.18f
    val sat = saturationMatrix(saturation.coerceIn(0f, 2.2f))
    val light = contrastBrightnessMatrix(
        contrast.coerceIn(0.4f, 2.2f),
        brightness.coerceIn(-0.6f, 0.6f),
    )
    val temp = warmthMatrix(warmth.coerceIn(-0.5f, 0.5f))
    val grayscale = if (edit.filter == ImageFilter.GRAYSCALE) saturationMatrix(0f) else identityMatrix()
    return concat(temp, concat(light, concat(sat, grayscale)))
}

private fun identityMatrix(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

private fun saturationMatrix(saturation: Float): FloatArray {
    val r = 0.213f * (1f - saturation)
    val g = 0.715f * (1f - saturation)
    val b = 0.072f * (1f - saturation)
    return floatArrayOf(
        r + saturation, g, b, 0f, 0f,
        r, g + saturation, b, 0f, 0f,
        r, g, b + saturation, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun contrastBrightnessMatrix(contrast: Float, brightness: Float): FloatArray {
    val t = (1f - contrast) / 2f * 255f
    val b = brightness * 255f
    return floatArrayOf(
        contrast, 0f, 0f, 0f, t + b,
        0f, contrast, 0f, 0f, t + b,
        0f, 0f, contrast, 0f, t + b,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun warmthMatrix(warmth: Float): FloatArray {
    val r = 1f + warmth * 0.35f
    val b = 1f - warmth * 0.35f
    return floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

private fun concat(a: FloatArray, b: FloatArray): FloatArray {
    val out = FloatArray(20)
    for (row in 0..3) {
        for (col in 0..3) {
            out[row * 5 + col] =
                a[row * 5] * b[col] +
                    a[row * 5 + 1] * b[5 + col] +
                    a[row * 5 + 2] * b[10 + col] +
                    a[row * 5 + 3] * b[15 + col]
        }
        out[row * 5 + 4] =
            a[row * 5] * b[4] +
                a[row * 5 + 1] * b[9] +
                a[row * 5 + 2] * b[14] +
                a[row * 5 + 3] * b[19] +
                a[row * 5 + 4]
    }
    return out
}

internal fun polyToPoly(src: FloatArray, dst: FloatArray): FloatArray? {
    if (src.size < 8 || dst.size < 8) return null
    val a = Array(8) { DoubleArray(8) }
    val b = DoubleArray(8)
    for (i in 0..3) {
        val x = src[i * 2].toDouble()
        val y = src[i * 2 + 1].toDouble()
        val u = dst[i * 2].toDouble()
        val v = dst[i * 2 + 1].toDouble()
        a[i * 2][0] = x
        a[i * 2][1] = y
        a[i * 2][2] = 1.0
        a[i * 2][6] = -u * x
        a[i * 2][7] = -u * y
        b[i * 2] = u
        a[i * 2 + 1][3] = x
        a[i * 2 + 1][4] = y
        a[i * 2 + 1][5] = 1.0
        a[i * 2 + 1][6] = -v * x
        a[i * 2 + 1][7] = -v * y
        b[i * 2 + 1] = v
    }
    val h = solveLinear(a, b) ?: return null
    return floatArrayOf(
        h[0].toFloat(), h[1].toFloat(), h[2].toFloat(),
        h[3].toFloat(), h[4].toFloat(), h[5].toFloat(),
        h[6].toFloat(), h[7].toFloat(), 1f,
    )
}

private fun solveLinear(a: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
    val n = b.size
    val m = Array(n) { row -> a[row].copyOf(n + 1) }
    for (row in 0 until n) m[row][n] = b[row]
    for (col in 0 until n) {
        var pivot = col
        var best = kotlin.math.abs(m[col][col])
        for (row in col + 1 until n) {
            val value = kotlin.math.abs(m[row][col])
            if (value > best) {
                best = value
                pivot = row
            }
        }
        if (best < 1e-10) return null
        if (pivot != col) {
            val tmp = m[col]
            m[col] = m[pivot]
            m[pivot] = tmp
        }
        val div = m[col][col]
        for (j in col..n) m[col][j] /= div
        for (row in 0 until n) {
            if (row == col) continue
            val factor = m[row][col]
            for (j in col..n) m[row][j] -= factor * m[col][j]
        }
    }
    return DoubleArray(n) { m[it][n] }
}

expect class ImageEditor() {
    fun apply(input: ByteArray, edit: ImageEdit, maxEdge: Int): ByteArray
}
