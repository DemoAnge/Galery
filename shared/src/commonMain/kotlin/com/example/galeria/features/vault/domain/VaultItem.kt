package com.example.galeria.features.vault.domain

enum class VaultMediaKind {
    IMAGE,
    VIDEO,
}

data class VaultItemId(val value: String) {
    init {
        require(value.isNotEmpty()) { "VaultItemId must not be empty" }
    }
}

/**
 * Metadatos de un ítem ya dentro del vault. Sin bytes ni rutas de archivo.
 */
data class VaultItem(
    val id: VaultItemId,
    val displayName: String,
    val mediaKind: VaultMediaKind,
    val capturedAtEpochMs: Long,
    val sizeBytes: Long,
    val addedAtEpochMs: Long,
    val albumLabel: String? = null,
)

/**
 * Medio aún visible en el almacenamiento público del sistema (MediaStore / Fotos).
 * [publicUri] no se incluye en [toString].
 */
data class ExternalMediaSource(
    val publicUri: String,
    val displayName: String,
    val mediaKind: VaultMediaKind,
    val capturedAtEpochMs: Long,
    val sizeBytes: Long,
    val albumLabel: String? = null,
) {
    override fun toString(): String =
        "ExternalMediaSource(displayName=$displayName, mediaKind=$mediaKind, sizeBytes=$sizeBytes)"
}
