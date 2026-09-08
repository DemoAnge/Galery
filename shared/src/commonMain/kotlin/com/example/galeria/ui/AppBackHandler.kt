package com.example.galeria.ui

import androidx.compose.runtime.Composable

@Composable
expect fun AppBackHandler(enabled: Boolean, onBack: () -> Unit)
