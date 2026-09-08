package com.example.galeria.features.gallery.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun MoveToVaultConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (count <= 1) "¿Mover al Vault?" else "¿Mover $count elementos al Vault?")
        },
        text = {
            Text(
                if (count <= 1) {
                    "Se borrará de Fotos y Archivos. Solo quedará en el Vault."
                } else {
                    "Se borrarán de Fotos y Archivos. Solo quedarán en el Vault."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Sí") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("No") }
        },
    )
}
