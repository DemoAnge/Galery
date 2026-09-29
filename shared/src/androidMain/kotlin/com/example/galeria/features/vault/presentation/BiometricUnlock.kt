package com.example.galeria.features.vault.presentation

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt as FrameworkBiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.example.galeria.core.AndroidAppContext

actual class BiometricUnlock actual constructor() {

    private var cancellation: CancellationSignal? = null
    private var authGeneration: Int = 0

    actual fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val context = runCatching { AndroidAppContext.require() }.getOrNull() ?: return false
        val manager = BiometricManager.from(context)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    actual fun wrap(dek: ByteArray, onResult: (ByteArray?) -> Unit) {
        authenticate(
            title = "Activar huella",
            subtitle = "Confirma tu huella para usarla como atajo. El PIN sigue activo.",
        ) { ok ->
            onResult(if (ok) FLAG else null)
        }
    }

    actual fun unwrap(wrapped: ByteArray, onResult: (ByteArray?) -> Unit) {
        authenticate(
            title = "Desbloquear Vault",
            subtitle = "Usa tu huella. Si no, entra con el PIN.",
        ) { ok ->
            onResult(if (ok) FLAG else null)
        }
    }

    actual fun cancel() {
        authGeneration += 1
        val current = cancellation
        cancellation = null
        runCatching { current?.cancel() }
    }

    private fun authenticate(
        title: String,
        subtitle: String,
        onResult: (Boolean) -> Unit,
    ) {
        val activity = usableActivity() ?: run {
            onResult(false)
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onResult(false)
            return
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MainHandler.post { authenticate(title, subtitle, onResult) }
            return
        }
        authenticateWithFramework(activity, title, subtitle, onResult)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun authenticateWithFramework(
        activity: Activity,
        title: String,
        subtitle: String,
        onResult: (Boolean) -> Unit,
    ) {
        val generation = authGeneration + 1
        authGeneration = generation
        runCatching { cancellation?.cancel() }
        cancellation = null
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val cancelSignal = CancellationSignal()
            cancellation = cancelSignal
            val builder = FrameworkBiometricPrompt.Builder(activity)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButton("Usar PIN", executor) { _, _ -> }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setConfirmationRequired(false)
            }
            builder.build().authenticate(
                cancelSignal,
                executor,
                object : FrameworkBiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: FrameworkBiometricPrompt.AuthenticationResult?,
                    ) {
                        finish(generation, true, onResult)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        finish(generation, false, onResult)
                    }
                },
            )
        } catch (_: Exception) {
            cancellation = null
            finish(generation, false, onResult)
        }
    }

    private fun finish(generation: Int, ok: Boolean, onResult: (Boolean) -> Unit) {
        if (generation != authGeneration) return
        authGeneration += 1
        cancellation = null
        onResult(ok)
    }

    private fun usableActivity(): Activity? {
        val activity = AndroidAppContext.activity ?: return null
        if (activity.isFinishing || activity.isDestroyed) return null
        return activity
    }

    private companion object {
        val FLAG = byteArrayOf(0x42)
        val MainHandler = Handler(Looper.getMainLooper())
    }
}
