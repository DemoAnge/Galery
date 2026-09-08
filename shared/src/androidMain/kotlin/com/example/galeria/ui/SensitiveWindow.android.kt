package com.example.galeria.ui

import android.view.WindowManager
import com.example.galeria.core.AndroidAppContext

actual fun setSensitiveWindow(enabled: Boolean) {
    val activity = AndroidAppContext.activity ?: return
    activity.runOnUiThread {
        val flags = WindowManager.LayoutParams.FLAG_SECURE
        if (enabled) {
            activity.window.addFlags(flags)
        } else {
            activity.window.clearFlags(flags)
        }
    }
}
