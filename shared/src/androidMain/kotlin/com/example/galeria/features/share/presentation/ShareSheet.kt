package com.example.galeria.features.share.presentation

import android.content.ClipData
import android.content.Intent
import androidx.core.net.toUri
import com.example.galeria.core.AndroidAppContext

actual class ShareSheet actual constructor() {

    actual fun share(request: ShareRequest) {
        val activity = AndroidAppContext.activity
            ?: error("Share requires an Activity")
        val uri = request.publicUri.toUri()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = request.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, "Compartir"))
    }
}
