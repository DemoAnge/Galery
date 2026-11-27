package com.example.galeria.features.share.presentation

import com.example.galeria.features.share.domain.ShareTarget

class ShareRequest(
    val publicUri: String,
    val mimeType: String,
) {
    override fun toString(): String = "ShareRequest(mimeType=$mimeType)"
}

expect class ShareSheet() {
    fun targets(requests: List<ShareRequest>): List<ShareTarget>
    fun share(requests: List<ShareRequest>)
    fun shareTo(requests: List<ShareRequest>, target: ShareTarget)
}
