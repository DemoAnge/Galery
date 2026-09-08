package com.example.galeria.features.editor.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.galeria.features.editor.domain.CropAspect
import com.example.galeria.features.editor.domain.CropQuad
import com.example.galeria.features.editor.domain.ImageEdit
import com.example.galeria.features.editor.domain.ImageFilter
import com.example.galeria.features.editor.domain.cropRectForAspect
import com.example.galeria.features.editor.domain.label
import com.example.galeria.features.editor.domain.pixelRatio
import com.example.galeria.features.editor.domain.withoutCrop
import com.example.galeria.ui.GaleriaIcons

private enum class EditorTool { CROP, ROTATE, FILTER, ADJUST, PERFECT }

@Composable
fun EditorScreen(
    preview: ImageBitmap?,
    busy: Boolean,
    error: String?,
    onEditChanged: (ImageEdit) -> Unit,
    onSave: (ImageEdit) -> Unit,
    onClose: () -> Unit,
) {
    var rotation by remember { mutableIntStateOf(0) }
    var straighten by remember { mutableFloatStateOf(0f) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var cropAspect by remember { mutableStateOf(CropAspect.FREE) }
    var crop by remember { mutableStateOf(CropQuad()) }
    var filter by remember { mutableStateOf(ImageFilter.NONE) }
    var autoPerfect by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var highlights by remember { mutableFloatStateOf(0f) }
    var shadows by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var warmth by remember { mutableFloatStateOf(0f) }
    var vignette by remember { mutableFloatStateOf(0f) }
    var tool by remember { mutableStateOf(EditorTool.CROP) }
    val edit = ImageEdit(
        rotationDegrees = rotation,
        straighten = straighten,
        flipHorizontal = flipH,
        flipVertical = flipV,
        cropAspect = cropAspect,
        crop = crop,
        filter = filter,
        autoPerfect = autoPerfect,
        brightness = brightness,
        contrast = contrast,
        highlights = highlights,
        shadows = shadows,
        saturation = saturation,
        warmth = warmth,
        vignette = vignette,
    )
    LaunchedEffect(edit, tool) {
        onEditChanged(if (tool == EditorTool.CROP) edit.withoutCrop() else edit)
    }

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
                "Edición profesional",
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
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = "Edición",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                if (tool == EditorTool.CROP) {
                    CropOverlay(
                        imageWidth = preview.width,
                        imageHeight = preview.height,
                        crop = crop,
                        aspect = cropAspect,
                        enabled = !busy,
                        onCropChange = { crop = it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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
        EditorToolPanel(
            tool = tool,
            rotation = rotation,
            straighten = straighten,
            crop = cropAspect,
            filter = filter,
            autoPerfect = autoPerfect,
            brightness = brightness,
            contrast = contrast,
            highlights = highlights,
            shadows = shadows,
            saturation = saturation,
            warmth = warmth,
            vignette = vignette,
            enabled = !busy,
            onRotateLeft = { rotation = (rotation + 270) % 360 },
            onRotateRight = { rotation = (rotation + 90) % 360 },
            onStraighten = { straighten = it },
            onFlipH = { flipH = !flipH },
            onFlipV = { flipV = !flipV },
            onCropAspect = { next ->
                cropAspect = next
                val ratio = next.pixelRatio(preview?.width ?: 1, preview?.height ?: 1)
                if (ratio != null) {
                    val imageAspect = if ((preview?.height ?: 0) > 0) {
                        preview!!.width.toFloat() / preview.height.toFloat()
                    } else {
                        1f
                    }
                    crop = CropQuad.fromRect(cropRectForAspect(imageAspect, ratio))
                } else {
                    crop = CropQuad.fromRect(crop.boundingRect())
                }
            },
            onFilter = { filter = it },
            onAutoPerfect = { autoPerfect = it },
            onBrightness = { brightness = it },
            onContrast = { contrast = it },
            onHighlights = { highlights = it },
            onShadows = { shadows = it },
            onSaturation = { saturation = it },
            onWarmth = { warmth = it },
            onVignette = { vignette = it },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditorToolTab(GaleriaIcons.Crop, "Recortar", tool == EditorTool.CROP) { tool = EditorTool.CROP }
            EditorToolTab(GaleriaIcons.Rotate, "Girar", tool == EditorTool.ROTATE) { tool = EditorTool.ROTATE }
            EditorToolTab(GaleriaIcons.PhotosOutline, "Filtros", tool == EditorTool.FILTER) { tool = EditorTool.FILTER }
            EditorToolTab(GaleriaIcons.Tune, "Ajustar", tool == EditorTool.ADJUST) { tool = EditorTool.ADJUST }
            EditorToolTab(GaleriaIcons.FavoriteFilled, "Perfección", tool == EditorTool.PERFECT) { tool = EditorTool.PERFECT }
        }
    }
}

@Composable
private fun EditorToolPanel(
    tool: EditorTool,
    rotation: Int,
    straighten: Float,
    crop: CropAspect,
    filter: ImageFilter,
    autoPerfect: Boolean,
    brightness: Float,
    contrast: Float,
    highlights: Float,
    shadows: Float,
    saturation: Float,
    warmth: Float,
    vignette: Float,
    enabled: Boolean,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onStraighten: (Float) -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onCropAspect: (CropAspect) -> Unit,
    onFilter: (ImageFilter) -> Unit,
    onAutoPerfect: (Boolean) -> Unit,
    onBrightness: (Float) -> Unit,
    onContrast: (Float) -> Unit,
    onHighlights: (Float) -> Unit,
    onShadows: (Float) -> Unit,
    onSaturation: (Float) -> Unit,
    onWarmth: (Float) -> Unit,
    onVignette: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .background(Color(0xFF1A1824))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        when (tool) {
            EditorTool.CROP -> {
                Text("Proporción", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CropAspect.entries.forEach { option ->
                        FilterChip(
                            selected = crop == option,
                            onClick = { if (enabled) onCropAspect(option) },
                            label = { Text(option.label()) },
                        )
                    }
                }
            }
            EditorTool.ROTATE -> Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TextButton(onClick = onRotateLeft, enabled = enabled) { Text("90° izq.", color = Color.White) }
                    TextButton(onClick = onRotateRight, enabled = enabled) { Text("90° der.", color = Color.White) }
                    TextButton(onClick = onFlipH, enabled = enabled) { Text("Voltear H", color = Color.White) }
                    TextButton(onClick = onFlipV, enabled = enabled) { Text("Voltear V", color = Color.White) }
                }
                Text("Enderezar  ${straighten.toInt()}°", color = Color.White.copy(alpha = 0.8f))
                Slider(value = straighten, onValueChange = onStraighten, valueRange = -45f..45f, enabled = enabled)
            }
            EditorTool.FILTER -> Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImageFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { if (enabled) onFilter(option) },
                        label = { Text(filterLabel(option)) },
                    )
                }
            }
            EditorTool.ADJUST -> Column(Modifier.verticalScroll(rememberScrollState())) {
                AdjustSlider("Brillo", brightness, -0.5f..0.5f, enabled, onBrightness)
                AdjustSlider("Contraste", contrast, 0.5f..1.8f, enabled, onContrast)
                AdjustSlider("Destellos", highlights, -0.5f..0.5f, enabled, onHighlights)
                AdjustSlider("Sombras", shadows, -0.5f..0.5f, enabled, onShadows)
                AdjustSlider("Saturación", saturation, 0f..1.8f, enabled, onSaturation)
                AdjustSlider("Calidez", warmth, -0.5f..0.5f, enabled, onWarmth)
                AdjustSlider("Viñeta", vignette, 0f..1f, enabled, onVignette)
            }
            EditorTool.PERFECT -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Perfección", color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Mejora automática de luz y color, como el modo profesional del teléfono.",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = { onAutoPerfect(!autoPerfect) },
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (autoPerfect) Color.White else Color(0xFF3D3358),
                        contentColor = if (autoPerfect) Color.Black else Color.White,
                    ),
                ) {
                    Text(if (autoPerfect) "Perfección activa" else "Aplicar Perfección")
                }
            }
        }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
    Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled)
}

private fun filterLabel(filter: ImageFilter): String = when (filter) {
    ImageFilter.NONE -> "Original"
    ImageFilter.VIVID -> "Vívido"
    ImageFilter.PORTRAIT -> "Retrato"
    ImageFilter.CINEMA -> "Cine"
    ImageFilter.COOL -> "Frío"
    ImageFilter.WARM -> "Cálido"
    ImageFilter.DRAMATIC -> "Dramático"
    ImageFilter.GRAYSCALE -> "B/N"
    ImageFilter.FILM -> "Película"
    ImageFilter.FADE -> "Fade"
}

@Composable
private fun EditorToolTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) Color.White else Color.White.copy(alpha = 0.55f)
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

expect fun decodeImageBytes(bytes: ByteArray): ImageBitmap?
