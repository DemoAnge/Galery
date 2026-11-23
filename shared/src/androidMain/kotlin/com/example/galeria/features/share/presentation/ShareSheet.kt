package com.example.galeria.features.share.presentation

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.example.galeria.core.AndroidAppContext
import com.example.galeria.features.share.domain.sharedMimeType

actual class ShareSheet actual constructor() {

    actual fun share(requests: List<ShareRequest>) {
        val activity = AndroidAppContext.activity
            ?: error("Share requires an Activity")
        check(requests.isNotEmpty()) { "Missing public media" }
        val uris = requests.map { it.publicUri.toUri() }
        val intent = if (uris.size == 1) {
            val uri = uris.first()
            Intent(Intent.ACTION_SEND).apply {
                type = requests.first().mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = sharedMimeType(requests.map { it.mimeType })
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(uris))
                clipData = ClipData.newRawUri("", uris.first()).also { clip ->
                    uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        activity.startActivity(Intent.createChooser(intent, "Compartir"))
    }
}
