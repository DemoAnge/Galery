package com.example.galeria.features.share.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.GalleryMediaKind
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.features.gallery.presentation.MediaThumb
import com.example.galeria.features.share.domain.ShareKind
import com.example.galeria.features.share.domain.ShareTarget
import com.example.galeria.features.share.domain.shareHeadline
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.MemoryCardRadius

@Composable
fun ShareDestinationsSheet(
    items: List<MediaItem>,
    targets: List<ShareTarget>,
    hint: String?,
    onShareTo: (ShareTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    val images = items.count { it.mediaKind == GalleryMediaKind.IMAGE }
    val videos = items.count { it.mediaKind == GalleryMediaKind.VIDEO }
    val quick = targets.filter { it.kind != ShareKind.APP }
    val apps = targets.filter { it.kind == ShareKind.APP }
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
            shape = RoundedCornerShape(topStart = MemoryCardRadius, topEnd = MemoryCardRadius),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Text(
                    text = shareHeadline(items.size, images, videos),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (items.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SharePreviewRow(items)
                }
                if (!hint.isNullOrBlank()) {
                    Text(
                        text = hint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                ShareTargetGrid(targets = quick, onShareTo = onShareTo)
                if (apps.isNotEmpty()) {
                    Text(
                        text = "Apps",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                    )
                    ShareTargetGrid(targets = apps, onShareTo = onShareTo)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SharePreviewRow(items: List<MediaItem>) {
    val shown = items.take(4)
    val extra = items.size - shown.size
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        shown.forEach { item ->
            MediaThumb(
                item = item,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                maxEdge = 160,
            )
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text("+$extra", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ShareTargetGrid(
    targets: List<ShareTarget>,
    onShareTo: (ShareTarget) -> Unit,
) {
    if (targets.isEmpty()) return
    val rows = ((targets.size + 3) / 4).coerceAtLeast(1)
    val height = (rows * 96).coerceAtMost(360).dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        userScrollEnabled = rows > 3,
    ) {
        items(targets, key = { it.id }) { target ->
            ShareTargetCell(target = target, onClick = { onShareTo(target) })
        }
    }
}

@Composable
private fun ShareTargetCell(
    target: ShareTarget,
    onClick: () -> Unit,
) {
    val tint = shareTargetColor(target.kind)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (target.enabled) 1f else 0.42f)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            val icon = shareTargetIcon(target.kind)
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = target.label,
                    tint = tint,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Text(
                    text = shareTargetLetter(target),
                    color = tint,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = target.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(72.dp),
        )
    }
}

private fun shareTargetColor(kind: ShareKind): Color = when (kind) {
    ShareKind.INSTAGRAM_STORY, ShareKind.INSTAGRAM -> Color(0xFFE1306C)
    ShareKind.FACEBOOK_STORY, ShareKind.FACEBOOK -> Color(0xFF1877F2)
    ShareKind.WHATSAPP -> Color(0xFF25D366)
    ShareKind.NEARBY -> Color(0xFF1A73E8)
    ShareKind.BLUETOOTH -> Color(0xFF0082FC)
    ShareKind.MORE -> Color(0xFF6B4FBF)
    ShareKind.APP -> Color(0xFF5F5A6B)
}

private fun shareTargetIcon(kind: ShareKind): ImageVector? = when (kind) {
    ShareKind.INSTAGRAM_STORY, ShareKind.FACEBOOK_STORY -> GaleriaIcons.Stories
    ShareKind.NEARBY -> GaleriaIcons.Nearby
    ShareKind.BLUETOOTH -> GaleriaIcons.Bluetooth
    ShareKind.WHATSAPP -> GaleriaIcons.Chat
    ShareKind.MORE -> GaleriaIcons.Share
    ShareKind.INSTAGRAM, ShareKind.FACEBOOK, ShareKind.APP -> null
}

private fun shareTargetLetter(target: ShareTarget): String = when (target.kind) {
    ShareKind.INSTAGRAM -> "Ig"
    ShareKind.FACEBOOK -> "f"
    else -> target.label.trim().take(1).ifBlank { "·" }.uppercase()
}
