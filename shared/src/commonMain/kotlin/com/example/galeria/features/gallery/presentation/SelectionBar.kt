package com.example.galeria.features.gallery.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.example.galeria.ui.GaleriaIcons

@Composable
fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onAddToAlbum: () -> Unit,
    onMoveToVault: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromAlbum: (() -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null,
) {
    var confirmMove by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(GaleriaIcons.Close, contentDescription = "Cancelar selección")
            }
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (onSelectAll != null) {
                TextButton(onClick = onSelectAll) {
                    Text("Todo")
                }
            }
            IconButton(onClick = onShare) {
                Icon(GaleriaIcons.Share, contentDescription = "Compartir")
            }
            if (onRemoveFromAlbum != null) {
                TextButton(onClick = onRemoveFromAlbum) {
                    Text("Quitar")
                }
            }
            IconButton(onClick = onAddToAlbum) {
                Icon(GaleriaIcons.Plus, contentDescription = "Mover al álbum")
            }
            IconButton(onClick = { confirmMove = true }) {
                Icon(GaleriaIcons.VaultOutline, contentDescription = "Mover al Vault")
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(GaleriaIcons.Delete, contentDescription = "Eliminar")
            }
        }
    }
    if (confirmMove) {
        MoveToVaultConfirmDialog(
            count = count,
            onConfirm = {
                confirmMove = false
                onMoveToVault()
            },
            onDismiss = { confirmMove = false },
        )
    }
    if (confirmDelete) {
        GalleryDeleteConfirmDialog(
            count = count,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
    }
}
