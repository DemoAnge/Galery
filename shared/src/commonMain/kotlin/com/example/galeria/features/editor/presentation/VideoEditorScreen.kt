package com.example.galeria.features.editor.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.galeria.features.editor.domain.MIN_TRIM_MS
import com.example.galeria.features.editor.domain.VideoEdit
import com.example.galeria.features.editor.domain.clamped
import com.example.galeria.features.editor.domain.rotatedBy
import com.example.galeria.features.gallery.presentation.formatDuration
import com.example.galeria.ui.GaleriaIcons
import com.example.galeria.ui.VideoPlayer

@Composable
fun VideoEditorScreen(
    uri: String,
    durationMs: Long,
    busy: Boolean,
    error: String?,
    onSave: (VideoEdit) -> Unit,
    onClose: () -> Unit,
) {
    var startMs by remember(uri) { mutableLongStateOf(0L) }
    var endMs by remember(uri) { mutableLongStateOf(durationMs.coerceAtLeast(0L)) }
    var rotation by remember(uri) { mutableIntStateOf(0) }
    val edit = VideoEdit(startMs = startMs, endMs = endMs, rotationDegrees = rotation)
        .clamped(durationMs)
    val duration = durationMs.coerceAtLeast(0L).toFloat()
    val canTrim = durationMs > MIN_TRIM_MS

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, enabled = !busy) {
                Icon(GaleriaIcons.Close, contentDescription = "Cerrar", tint = Color.White)
            }
            Text(
                "Editar video",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onSave(edit) }, enabled = !busy) {
                Text(if (busy) "Guardando…" else "Guardar", color = Color.White)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (uri.isNotBlank()) {
                VideoPlayer(
                    uri = uri,
                    playWhenReady = !busy,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = edit.rotationDegrees.toFloat() },
                )
            } else {
                Text("Preparando vista previa…", color = Color.White)
            }
        }
        if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1824))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { rotation = VideoEdit(rotationDegrees = rotation).rotatedBy(-90).rotationDegrees },
                    enabled = !busy,
                ) { Text("90° izq.", color = Color.White) }
                Text(
                    "${edit.rotationDegrees}°",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                )
                TextButton(
                    onClick = { rotation = VideoEdit(rotationDegrees = rotation).rotatedBy(90).rotationDegrees },
                    enabled = !busy,
                ) { Text("90° der.", color = Color.White) }
            }
            Text(
                "Recortar",
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "${formatDuration(edit.startMs)} – ${formatDuration(edit.endMs)}",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            Text("Inicio", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = edit.startMs.toFloat().coerceIn(0f, duration.coerceAtLeast(0f)),
                onValueChange = { startMs = it.toLong() },
                valueRange = 0f..duration.coerceAtLeast(0f),
                enabled = !busy && canTrim,
            )
            Text("Fin", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
            Slider(
                value = edit.endMs.toFloat().coerceIn(0f, duration.coerceAtLeast(0f)),
                onValueChange = { endMs = it.toLong() },
                valueRange = 0f..duration.coerceAtLeast(0f),
                enabled = !busy && canTrim,
            )
        }
    }
}
