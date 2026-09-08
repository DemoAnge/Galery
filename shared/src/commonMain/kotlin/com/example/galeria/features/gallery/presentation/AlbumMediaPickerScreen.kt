package com.example.galeria.features.gallery.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.galeria.features.gallery.domain.MediaItem
import com.example.galeria.ui.GaleriaIcons

@Composable
fun AlbumMediaPickerScreen(
    items: List<MediaItem>,
    albumTitle: String,
    allowSkip: Boolean,
    onConfirm: (List<String>) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(GaleriaIcons.Close, contentDescription = "Cancelar")
                }
                Column(Modifier.weight(1f)) {
                    Text("Mover al álbum", style = MaterialTheme.typography.titleLarge)
                    Text(
                        albumTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (allowSkip) {
                    TextButton(onClick = onSkip) { Text("Omitir") }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                DatedPhotoGrid(
                    items = items,
                    selectedIds = selectedIds,
                    selecting = true,
                    onOpen = { item, _ ->
                        selectedIds = if (item.id in selectedIds) {
                            selectedIds - item.id
                        } else {
                            selectedIds + item.id
                        }
                    },
                    onLongPress = { item ->
                        selectedIds = if (item.id in selectedIds) {
                            selectedIds - item.id
                        } else {
                            selectedIds + item.id
                        }
                    },
                    onToggleSelect = { item ->
                        selectedIds = if (item.id in selectedIds) {
                            selectedIds - item.id
                        } else {
                            selectedIds + item.id
                        }
                    },
                    emptyTitle = "Sin fotos todavía",
                    emptyBody = "No hay elementos en el dispositivo para mover a este álbum.",
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (selectedIds.isEmpty()) "Elige fotos o videos" else "${selectedIds.size} seleccionados",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onConfirm(selectedIds.toList()) },
                    enabled = selectedIds.isNotEmpty(),
                ) {
                    Text("Mover")
                }
            }
        }
    }
}
