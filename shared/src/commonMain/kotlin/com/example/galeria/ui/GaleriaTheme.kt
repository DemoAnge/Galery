package com.example.galeria.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PhotoGridGap = 2.dp
val AlbumCardRadius = 22.dp
val AlbumCoverRadius = 20.dp
val TileRadius = 8.dp
val MemoryCardRadius = 28.dp
val MemoryCardHeight = 196.dp
val AccentsLavender = Color(0xFFC9B6FF)

private val LightPrimary = Color(0xFF6B4FBF)
private val DarkPrimary = AccentsLavender

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE4FF),
    onPrimaryContainer = Color(0xFF2A2140),
    secondary = Color(0xFF5F5A6B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0ECF6),
    onSecondaryContainer = Color(0xFF2A2140),
    background = Color(0xFFF7F5FB),
    onBackground = Color(0xFF1C1826),
    surface = Color(0xFFF7F5FB),
    onSurface = Color(0xFF1C1826),
    surfaceVariant = Color(0xFFE8E2F0),
    onSurfaceVariant = Color(0xFF5F5A6B),
    surfaceContainer = Color(0xFFEEEAF4),
    surfaceContainerHigh = Color(0xFFE4DEEC),
    surfaceContainerHighest = Color(0xFFD9D2E4),
    surfaceContainerLow = Color(0xFFF3F0F7),
    outline = Color(0xFFD4CDE0),
    outlineVariant = Color(0xFFE8E2F0),
    error = Color(0xFFD93025),
    onError = Color.White,
    inverseSurface = Color(0xFF2A2438),
    inverseOnSurface = Color(0xFFF3F0F7),
    inversePrimary = DarkPrimary,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF2A2140),
    primaryContainer = Color(0xFF3D3358),
    onPrimaryContainer = AccentsLavender,
    secondary = Color(0xFFB8B0C8),
    onSecondary = Color(0xFF1C1826),
    secondaryContainer = Color(0xFF2E2A3C),
    onSecondaryContainer = Color(0xFFE8E2F0),
    background = Color(0xFF16141F),
    onBackground = Color(0xFFF3F0F7),
    surface = Color(0xFF16141F),
    onSurface = Color(0xFFF3F0F7),
    surfaceVariant = Color(0xFF2A2636),
    onSurfaceVariant = Color(0xFFB8B0C8),
    surfaceContainer = Color(0xFF1E1B28),
    surfaceContainerHigh = Color(0xFF262232),
    surfaceContainerHighest = Color(0xFF2E2A3C),
    surfaceContainerLow = Color(0xFF1A1824),
    outline = Color(0xFF4A4458),
    outlineVariant = Color(0xFF2E2A3C),
    error = Color(0xFFF28B82),
    onError = Color(0xFF3B0A0A),
    inverseSurface = Color(0xFFF3F0F7),
    inverseOnSurface = Color(0xFF2A2438),
    inversePrimary = LightPrimary,
)

private val GaleriaTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
fun GaleriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = GaleriaTypography,
        content = content,
    )
}
