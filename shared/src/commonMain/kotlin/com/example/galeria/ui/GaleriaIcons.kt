package com.example.galeria.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object GaleriaIcons {
    val PhotosOutline: ImageVector by lazy {
        strokeIcon("PhotosOutline") {
            moveTo(5f, 5f)
            lineTo(10.5f, 5f)
            lineTo(10.5f, 10.5f)
            lineTo(5f, 10.5f)
            close()
            moveTo(13.5f, 5f)
            lineTo(19f, 5f)
            lineTo(19f, 10.5f)
            lineTo(13.5f, 10.5f)
            close()
            moveTo(5f, 13.5f)
            lineTo(10.5f, 13.5f)
            lineTo(10.5f, 19f)
            lineTo(5f, 19f)
            close()
            moveTo(13.5f, 13.5f)
            lineTo(19f, 13.5f)
            lineTo(19f, 19f)
            lineTo(13.5f, 19f)
            close()
        }
    }

    val PhotosFilled: ImageVector by lazy {
        fillIcon("PhotosFilled") {
            moveTo(4f, 4f)
            horizontalLineTo(11f)
            verticalLineTo(11f)
            horizontalLineTo(4f)
            close()
            moveTo(13f, 4f)
            horizontalLineTo(20f)
            verticalLineTo(11f)
            horizontalLineTo(13f)
            close()
            moveTo(4f, 13f)
            horizontalLineTo(11f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(13f, 13f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(13f)
            close()
        }
    }

    val SearchOutline: ImageVector by lazy {
        strokeIcon("SearchOutline") {
            moveTo(11f, 5.5f)
            arcToRelative(5.5f, 5.5f, 0f, true, true, 0f, 11f)
            arcToRelative(5.5f, 5.5f, 0f, true, true, 0f, -11f)
            close()
            moveTo(15.5f, 15.5f)
            lineTo(20f, 20f)
        }
    }

    val SearchFilled: ImageVector by lazy {
        fillIcon("SearchFilled") {
            moveTo(15.5f, 14f)
            horizontalLineTo(14.71f)
            lineTo(14.43f, 13.73f)
            curveTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
            curveTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
            curveTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
            curveTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
            curveTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
            lineTo(14f, 14.71f)
            verticalLineTo(15.5f)
            lineTo(19f, 20.49f)
            lineTo(20.49f, 19f)
            lineTo(15.5f, 14f)
            close()
            moveTo(9.5f, 14f)
            curveTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
            curveTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
            curveTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
            curveTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
            close()
        }
    }

    val VaultOutline: ImageVector by lazy {
        strokeIcon("VaultOutline") {
            moveTo(8f, 11f)
            verticalLineTo(8f)
            arcToRelative(4f, 4f, 0f, false, true, 8f, 0f)
            verticalLineTo(11f)
            moveTo(6.5f, 11f)
            horizontalLineTo(17.5f)
            verticalLineTo(20f)
            horizontalLineTo(6.5f)
            close()
        }
    }

    val VaultFilled: ImageVector by lazy {
        fillIcon("VaultFilled") {
            moveTo(18f, 8f)
            horizontalLineTo(17f)
            verticalLineTo(6f)
            curveTo(17f, 3.24f, 14.76f, 1f, 12f, 1f)
            curveTo(9.24f, 1f, 7f, 3.24f, 7f, 6f)
            verticalLineTo(8f)
            horizontalLineTo(6f)
            curveTo(4.9f, 8f, 4f, 8.9f, 4f, 10f)
            verticalLineTo(20f)
            curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
            horizontalLineTo(18f)
            curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
            verticalLineTo(10f)
            curveTo(20f, 8.9f, 19.1f, 8f, 18f, 8f)
            close()
            moveTo(12f, 17f)
            curveTo(10.9f, 17f, 10f, 16.1f, 10f, 15f)
            curveTo(10f, 13.9f, 10.9f, 13f, 12f, 13f)
            curveTo(13.1f, 13f, 14f, 13.9f, 14f, 15f)
            curveTo(14f, 16.1f, 13.1f, 17f, 12f, 17f)
            close()
            moveTo(15.1f, 8f)
            horizontalLineTo(8.9f)
            verticalLineTo(6f)
            curveTo(8.9f, 4.29f, 10.29f, 2.9f, 12f, 2.9f)
            curveTo(13.71f, 2.9f, 15.1f, 4.29f, 15.1f, 6f)
            verticalLineTo(8f)
            close()
        }
    }

    val LibraryOutline: ImageVector by lazy {
        strokeIcon("LibraryOutline") {
            moveTo(5f, 7f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            horizontalLineTo(5f)
            close()
            moveTo(8f, 5f)
            horizontalLineTo(20f)
            moveTo(11f, 3f)
            horizontalLineTo(21f)
        }
    }

    val LibraryFilled: ImageVector by lazy {
        fillIcon("LibraryFilled") {
            moveTo(4f, 6f)
            horizontalLineTo(16f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(6f, 2f)
            horizontalLineTo(20f)
            curveTo(21.1f, 2f, 22f, 2.9f, 22f, 4f)
            verticalLineTo(16f)
            horizontalLineTo(20f)
            verticalLineTo(4f)
            horizontalLineTo(6f)
            verticalLineTo(2f)
            close()
        }
    }

    val Back: ImageVector by lazy {
        strokeIcon("Back", width = 2.2f) {
            moveTo(15f, 6f)
            lineTo(9f, 12f)
            lineTo(15f, 18f)
        }
    }

    val Share: ImageVector by lazy {
        strokeIcon("Share") {
            moveTo(16f, 8f)
            arcToRelative(2.2f, 2.2f, 0f, true, true, 0f, 0.01f)
            close()
            moveTo(8f, 12f)
            arcToRelative(2.2f, 2.2f, 0f, true, true, 0f, 0.01f)
            close()
            moveTo(16f, 16f)
            arcToRelative(2.2f, 2.2f, 0f, true, true, 0f, 0.01f)
            close()
            moveTo(10f, 11.2f)
            lineTo(14f, 9f)
            moveTo(10f, 12.8f)
            lineTo(14f, 15f)
        }
    }

    val Edit: ImageVector by lazy {
        strokeIcon("Edit") {
            moveTo(5f, 19f)
            lineTo(8.2f, 18.2f)
            lineTo(18f, 8.4f)
            lineTo(15.6f, 6f)
            lineTo(5.8f, 15.8f)
            close()
            moveTo(14.4f, 7.2f)
            lineTo(16.8f, 9.6f)
        }
    }

    val More: ImageVector by lazy {
        fillIcon("More") {
            moveTo(12f, 8f)
            arcToRelative(1.6f, 1.6f, 0f, true, true, 0f, 0.01f)
            close()
            moveTo(12f, 12f)
            arcToRelative(1.6f, 1.6f, 0f, true, true, 0f, 0.01f)
            close()
            moveTo(12f, 16f)
            arcToRelative(1.6f, 1.6f, 0f, true, true, 0f, 0.01f)
            close()
        }
    }

    val FavoriteOutline: ImageVector by lazy {
        strokeIcon("FavoriteOutline") {
            moveTo(12f, 19.4f)
            lineTo(10.55f, 18.07f)
            curveTo(6.4f, 14.36f, 4f, 12.28f, 4f, 9.5f)
            curveTo(4f, 7.5f, 5.5f, 6f, 7.5f, 6f)
            curveTo(8.74f, 6f, 9.89f, 6.62f, 10.6f, 7.6f)
            horizontalLineTo(13.4f)
            curveTo(14.11f, 6.62f, 15.26f, 6f, 16.5f, 6f)
            curveTo(18.5f, 6f, 20f, 7.5f, 20f, 9.5f)
            curveTo(20f, 12.28f, 17.6f, 14.36f, 13.45f, 18.08f)
            close()
        }
    }

    val FavoriteFilled: ImageVector by lazy {
        fillIcon("FavoriteFilled") {
            moveTo(12f, 21.35f)
            lineTo(10.55f, 20.03f)
            curveTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
            curveTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
            curveTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
            curveTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
            curveTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
            curveTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
            close()
        }
    }

    val Delete: ImageVector by lazy {
        strokeIcon("Delete") {
            moveTo(6f, 8f)
            horizontalLineTo(18f)
            verticalLineTo(19.5f)
            arcToRelative(1.5f, 1.5f, 0f, false, true, -1.5f, 1.5f)
            horizontalLineTo(7.5f)
            arcToRelative(1.5f, 1.5f, 0f, false, true, -1.5f, -1.5f)
            close()
            moveTo(9f, 4f)
            horizontalLineTo(15f)
            moveTo(4f, 8f)
            horizontalLineTo(20f)
            moveTo(10f, 11f)
            verticalLineTo(17f)
            moveTo(14f, 11f)
            verticalLineTo(17f)
        }
    }

    val Check: ImageVector by lazy {
        strokeIcon("Check", width = 2.2f) {
            moveTo(6f, 12f)
            lineTo(10f, 16f)
            lineTo(18f, 8f)
        }
    }

    val Close: ImageVector by lazy {
        strokeIcon("Close", width = 2f) {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }

    val Play: ImageVector by lazy {
        fillIcon("Play") {
            moveTo(9f, 7f)
            lineTo(18f, 12f)
            lineTo(9f, 17f)
            close()
        }
    }

    val VideosOutline: ImageVector by lazy {
        strokeIcon("VideosOutline") {
            moveTo(3.5f, 7.5f)
            horizontalLineTo(14.5f)
            verticalLineTo(16.5f)
            horizontalLineTo(3.5f)
            close()
            moveTo(16f, 10f)
            lineTo(20.5f, 8f)
            verticalLineTo(16f)
            lineTo(16f, 14f)
            close()
        }
    }

    val VideosFilled: ImageVector by lazy {
        fillIcon("VideosFilled") {
            moveTo(3f, 7f)
            horizontalLineTo(15f)
            verticalLineTo(17f)
            horizontalLineTo(3f)
            close()
            moveTo(16f, 9.5f)
            lineTo(21f, 7f)
            verticalLineTo(17f)
            lineTo(16f, 14.5f)
            close()
        }
    }

    val Cloud: ImageVector by lazy {
        strokeIcon("Cloud") {
            moveTo(7.5f, 18f)
            horizontalLineTo(16.5f)
            curveTo(18.4f, 18f, 20f, 16.4f, 20f, 14.5f)
            curveTo(20f, 12.6f, 18.4f, 11f, 16.5f, 11f)
            curveTo(16.3f, 8.5f, 14.2f, 6.5f, 11.6f, 6.5f)
            curveTo(9.2f, 6.5f, 7.2f, 8.2f, 6.7f, 10.5f)
            curveTo(5.1f, 10.7f, 4f, 12.1f, 4f, 13.7f)
            curveTo(4f, 15.4f, 5.4f, 16.8f, 7.1f, 16.9f)
        }
    }

    val Bell: ImageVector by lazy {
        strokeIcon("Bell") {
            moveTo(6f, 17f)
            horizontalLineTo(18f)
            lineTo(16.5f, 15.5f)
            verticalLineTo(11f)
            curveTo(16.5f, 8.5f, 14.7f, 6.4f, 12.3f, 6.1f)
            verticalLineTo(5.5f)
            arcToRelative(0.8f, 0.8f, 0f, true, false, -1.6f, 0f)
            verticalLineTo(6.1f)
            curveTo(8.3f, 6.4f, 6.5f, 8.5f, 6.5f, 11f)
            verticalLineTo(15.5f)
            close()
            moveTo(10f, 17f)
            curveTo(10f, 18.1f, 10.9f, 19f, 12f, 19f)
            curveTo(13.1f, 19f, 14f, 18.1f, 14f, 17f)
        }
    }

    val Plus: ImageVector by lazy {
        strokeIcon("Plus", width = 2f) {
            moveTo(12f, 6f)
            verticalLineTo(18f)
            moveTo(6f, 12f)
            horizontalLineTo(18f)
        }
    }

    val Stack: ImageVector by lazy {
        strokeIcon("Stack") {
            moveTo(6f, 9f)
            horizontalLineTo(16f)
            verticalLineTo(19f)
            horizontalLineTo(6f)
            close()
            moveTo(8f, 7f)
            horizontalLineTo(18f)
            verticalLineTo(9f)
            moveTo(10f, 5f)
            horizontalLineTo(20f)
            verticalLineTo(7f)
        }
    }

    val CollectionsOutline: ImageVector by lazy {
        strokeIcon("CollectionsOutline") {
            moveTo(5f, 8f)
            horizontalLineTo(15f)
            verticalLineTo(18f)
            horizontalLineTo(5f)
            close()
            moveTo(9f, 6f)
            horizontalLineTo(19f)
            verticalLineTo(8f)
            moveTo(8f, 11f)
            horizontalLineTo(12f)
            moveTo(8f, 14f)
            horizontalLineTo(14f)
        }
    }

    val CollectionsFilled: ImageVector by lazy {
        fillIcon("CollectionsFilled") {
            moveTo(4f, 7f)
            horizontalLineTo(16f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(7f, 4f)
            horizontalLineTo(20f)
            verticalLineTo(17f)
            horizontalLineTo(18f)
            verticalLineTo(6f)
            horizontalLineTo(7f)
            close()
        }
    }

    val CreateOutline: ImageVector by lazy {
        strokeIcon("CreateOutline") {
            moveTo(12f, 5f)
            verticalLineTo(19f)
            moveTo(5f, 12f)
            horizontalLineTo(19f)
        }
    }

    val Crop: ImageVector by lazy {
        strokeIcon("Crop") {
            moveTo(6f, 3f)
            verticalLineTo(15f)
            horizontalLineTo(18f)
            moveTo(3f, 6f)
            horizontalLineTo(15f)
            verticalLineTo(21f)
        }
    }

    val Rotate: ImageVector by lazy {
        strokeIcon("Rotate") {
            moveTo(4f, 12f)
            arcToRelative(8f, 8f, 0f, true, true, 2.2f, 5.5f)
            moveTo(4f, 12f)
            verticalLineTo(7f)
            moveTo(4f, 12f)
            horizontalLineTo(9f)
        }
    }

    val Tune: ImageVector by lazy {
        strokeIcon("Tune") {
            moveTo(4f, 8f)
            horizontalLineTo(20f)
            moveTo(8f, 8f)
            verticalLineTo(6f)
            moveTo(8f, 8f)
            verticalLineTo(10f)
            moveTo(4f, 16f)
            horizontalLineTo(20f)
            moveTo(16f, 16f)
            verticalLineTo(14f)
            moveTo(16f, 16f)
            verticalLineTo(18f)
        }
    }

    val Fingerprint: ImageVector by lazy {
        strokeIcon("Fingerprint") {
            moveTo(12f, 11f)
            verticalLineTo(16.5f)
            moveTo(9.5f, 12f)
            verticalLineTo(15f)
            moveTo(14.5f, 12.2f)
            verticalLineTo(17.5f)
            moveTo(7.5f, 13.5f)
            verticalLineTo(16f)
            moveTo(12f, 7.2f)
            arcToRelative(4.8f, 4.8f, 0f, false, true, 4.8f, 4.8f)
            moveTo(12f, 7.2f)
            arcToRelative(4.8f, 4.8f, 0f, false, false, -3.6f, 1.5f)
            moveTo(6.8f, 9.6f)
            arcToRelative(6.5f, 6.5f, 0f, false, true, 10.4f, 2.2f)
        }
    }

    val Stories: ImageVector by lazy {
        strokeIcon("Stories") {
            moveTo(12f, 4.6f)
            arcToRelative(7.4f, 7.4f, 0f, true, true, 0f, 0.01f)
            close()
            moveTo(12f, 8.2f)
            arcToRelative(3.8f, 3.8f, 0f, true, true, 0f, 0.01f)
            close()
        }
    }

    val Nearby: ImageVector by lazy {
        strokeIcon("Nearby") {
            moveTo(12f, 18.2f)
            arcToRelative(1.1f, 1.1f, 0f, true, true, 0f, 0.01f)
            close()
            moveTo(8.4f, 14.4f)
            arcToRelative(5.2f, 5.2f, 0f, false, true, 7.2f, 0f)
            moveTo(5.8f, 11.4f)
            arcToRelative(8.8f, 8.8f, 0f, false, true, 12.4f, 0f)
            moveTo(3.4f, 8.4f)
            arcToRelative(12.2f, 12.2f, 0f, false, true, 17.2f, 0f)
        }
    }

    val Bluetooth: ImageVector by lazy {
        strokeIcon("Bluetooth") {
            moveTo(7f, 7.2f)
            lineTo(16.5f, 16.2f)
            lineTo(12f, 20.2f)
            lineTo(12f, 3.8f)
            lineTo(16.5f, 7.8f)
            lineTo(7f, 16.8f)
        }
    }

    val Chat: ImageVector by lazy {
        strokeIcon("Chat") {
            moveTo(5.5f, 6.5f)
            lineTo(18.5f, 6.5f)
            lineTo(18.5f, 15.2f)
            lineTo(13.2f, 15.2f)
            lineTo(8.8f, 18.8f)
            lineTo(8.8f, 15.2f)
            lineTo(5.5f, 15.2f)
            close()
        }
    }

    val Vault: ImageVector get() = VaultFilled
    val Gallery: ImageVector get() = PhotosFilled
}

private fun fillIcon(name: String, block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero,
            pathBuilder = block,
        )
    }.build()
}

private fun strokeIcon(
    name: String,
    width: Float = 1.7f,
    block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector {
    return ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
            pathBuilder = block,
        )
    }.build()
}
