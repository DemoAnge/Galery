package com.example.galeria.features.vault.presentation

internal expect fun writeTempPlayableVideo(
    itemId: String,
    displayName: String,
    bytes: ByteArray,
): String

internal expect fun deleteTempPlayableVideo(path: String)
