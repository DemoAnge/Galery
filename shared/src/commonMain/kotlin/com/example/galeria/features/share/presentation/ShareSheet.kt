package com.example.galeria.features.share.presentation

class ShareRequest(
    val publicUri: String,
    val mimeType: String,
) {
    override fun toString(): String = "ShareRequest(mimeType=$mimeType)"
}

expect class ShareSheet() {
    fun share(requests: List<ShareRequest>)
}
