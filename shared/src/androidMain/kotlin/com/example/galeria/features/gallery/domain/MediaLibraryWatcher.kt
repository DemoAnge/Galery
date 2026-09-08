package com.example.galeria.features.gallery.domain

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.galeria.core.AndroidAppContext

actual class MediaLibraryWatcher actual constructor() {
    private var observer: ContentObserver? = null
    private var onChanged: (() -> Unit)? = null
    private var handler: Handler? = null
    private var deliver: Runnable? = null
    private var ignoreNextResume = true
    private val onResume: () -> Unit = {
        if (ignoreNextResume) {
            ignoreNextResume = false
        } else {
            onChanged?.invoke()
        }
    }

    actual fun start(onChanged: () -> Unit) {
        stop()
        this.onChanged = onChanged
        ignoreNextResume = true
        val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return
        val mainHandler = Handler(Looper.getMainLooper())
        val deliverChange = Runnable { this.onChanged?.invoke() }
        handler = mainHandler
        deliver = deliverChange
        val obs = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean) {
                schedule()
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                schedule()
            }

            private fun schedule() {
                mainHandler.removeCallbacks(deliverChange)
                mainHandler.postDelayed(deliverChange, 400L)
            }
        }
        runCatching {
            val resolver = context.contentResolver
            resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, obs)
            resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, obs)
        }.onFailure {
            return
        }
        observer = obs
        AndroidAppContext.addResumeListener(onResume)
    }

    actual fun stop() {
        val obs = observer
        observer = null
        onChanged = null
        deliver?.let { handler?.removeCallbacks(it) }
        deliver = null
        handler = null
        AndroidAppContext.removeResumeListener(onResume)
        if (obs != null) {
            runCatching {
                AndroidAppContext.require().contentResolver.unregisterContentObserver(obs)
            }
        }
        ignoreNextResume = true
    }
}
