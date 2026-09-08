package com.example.galeria.ui

actual fun setSensitiveWindow(enabled: Boolean) {
    // iOS no expone un equivalente a FLAG_SECURE para toda la ventana.
}
