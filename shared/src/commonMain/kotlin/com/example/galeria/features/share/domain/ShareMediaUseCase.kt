package com.example.galeria.features.share.domain

import com.example.galeria.features.share.presentation.ShareRequest
import com.example.galeria.features.share.presentation.ShareSheet

/**
 * Comparte un medio ya público (galería o exportado del vault).
 * No debe usarse con ítems que siguen dentro del vault.
 */
class ShareMediaUseCase(
    private val shareSheet: ShareSheet,
) {
    operator fun invoke(publicUri: String, mimeType: String) {
        require(publicUri.isNotBlank()) { "Missing public media" }
        require(mimeType.isNotBlank()) { "Missing mime type" }
        shareSheet.share(ShareRequest(publicUri = publicUri, mimeType = mimeType))
    }
}
