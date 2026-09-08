package com.example.galeria.features.gallery.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.TileRadius

@Composable
fun CreateScreen(
    lastItem: MediaItem?,
    weekendItems: List<MediaItem>,
    onEditLast: () -> Unit,
    onOpenWeekend: () -> Unit,
    onCreateAlbum: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Crear", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Álbumes, recuerdos y edición.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CreateRow(
            icon = GaleriaIcons.Plus,
            title = "Nuevo álbum o colección",
            subtitle = "Crea uno y muévele fotos o videos",
            enabled = true,
            onClick = onCreateAlbum,
        )
        CreateRow(
            icon = GaleriaIcons.Edit,
            title = "Editar última foto",
            subtitle = lastItem?.displayName ?: "No hay fotos todavía",
            enabled = lastItem != null,
            onClick = onEditLast,
        )
        CreateRow(
            icon = GaleriaIcons.CollectionsOutline,
            title = "Dump del fin de semana",
            subtitle = if (weekendItems.isEmpty()) "Aún no hay recuerdos recientes" else "${weekendItems.size} elementos",
            enabled = weekendItems.isNotEmpty(),
            onClick = onOpenWeekend,
        )
    }
}

@Composable
private fun CreateRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(TileRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
