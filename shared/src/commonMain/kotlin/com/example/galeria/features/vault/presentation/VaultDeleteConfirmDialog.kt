package com.example.galeria.features.vault.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun VaultDeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (count <= 1) "¿Borrar del Vault?" else "¿Borrar $count elementos del Vault?")
        },
        text = {
            Text("Se borra del disco. No vuelve a Fotos y no se puede recuperar.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Borrar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
