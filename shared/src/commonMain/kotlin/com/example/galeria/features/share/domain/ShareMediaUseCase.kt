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
        invoke(listOf(ShareRequest(publicUri = publicUri, mimeType = mimeType)))
    }

    operator fun invoke(requests: List<ShareRequest>) {
        require(requests.isNotEmpty()) { "Missing public media" }
        requests.forEach { request ->
            require(request.publicUri.isNotBlank()) { "Missing public media" }
            require(request.mimeType.isNotBlank()) { "Missing mime type" }
        }
        shareSheet.share(requests)
    }
}

internal fun sharedMimeType(types: List<String>): String =
    types.distinct().singleOrNull() ?: "*/*"
