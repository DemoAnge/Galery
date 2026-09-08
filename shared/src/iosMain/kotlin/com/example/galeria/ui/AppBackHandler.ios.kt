package com.example.galeria.ui

import androidx.compose.runtime.Composable

@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses the Compose swipe-back / close gestures in each screen.
}
