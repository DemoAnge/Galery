package com.example.galeria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GaleriaBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        content()
    }
}

@Composable
fun GlassScrim(
    modifier: Modifier = Modifier,
    shape: Shape,
    glow: Color = AccentsLavender,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f),
                    0.38f to Color.Transparent,
                    0.72f to Color.Black.copy(alpha = 0.28f),
                    1f to Color.Black.copy(alpha = 0.82f),
                ),
            )
            .border(Dp.Hairline, glow.copy(alpha = 0.28f), shape),
    )
}
