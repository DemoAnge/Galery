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
        validate(requests)
        shareSheet.share(requests)
    }

    fun destinations(requests: List<ShareRequest>): List<ShareTarget> {
        validate(requests)
        val targets = shareSheet.targets(requests)
        return targets.ifEmpty { listOf(ShareTarget.MORE) }
    }

    fun shareTo(requests: List<ShareRequest>, target: ShareTarget) {
        validate(requests)
        require(target.enabled) { "Share target unavailable" }
        shareSheet.shareTo(requests, target)
    }

    private fun validate(requests: List<ShareRequest>) {
        require(requests.isNotEmpty()) { "Missing public media" }
        requests.forEach { request ->
            require(request.publicUri.isNotBlank()) { "Missing public media" }
            require(request.mimeType.isNotBlank()) { "Missing mime type" }
        }
    }
}

internal fun sharedMimeType(types: List<String>): String =
    types.distinct().singleOrNull() ?: "*/*"
