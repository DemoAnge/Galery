package com.example.galeria.core

/**
 * Contrato entre gallery y vault. Gallery no importa features.vault;
 * vault implementa esta interfaz en data.
 */
interface HideInVault {
    fun isReady(): Boolean
    fun isConfigured(): Boolean
    fun hide(media: PublicMediaRef)
    fun hideAll(media: List<PublicMediaRef>)
}

enum class CoreMediaKind {
    IMAGE,
    VIDEO,
}

class PublicMediaRef(
    val publicUri: String,
    val displayName: String,
    val mediaKind: CoreMediaKind,
    val capturedAtEpochMs: Long,
    val sizeBytes: Long,
    val albumLabel: String? = null,
) {
    override fun toString(): String =
        "PublicMediaRef(displayName=$displayName, mediaKind=$mediaKind, sizeBytes=$sizeBytes)"
}
