package com.example.galeria.features.gallery.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.galeria.core.AndroidAppContext

actual class MediaPermission actual constructor() {

    actual fun hasAccess(): Boolean {
        val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return false
        return requiredPermissions().any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        } || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ) == PackageManager.PERMISSION_GRANTED)
    }

    actual fun request(onResult: (Boolean) -> Unit) {
        if (hasAccess()) {
            onResult(true)
            return
        }
        val activity = AndroidAppContext.activity as? ComponentActivity ?: run {
            onResult(false)
            return
        }
        val key = "media-perm-${System.nanoTime()}"
        lateinit var launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
        launcher = activity.activityResultRegistry.register(
            key,
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { _ ->
            launcher.unregister()
            onResult(hasAccess())
        }
        launcher.launch(requiredPermissions())
    }
}

private fun requiredPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
