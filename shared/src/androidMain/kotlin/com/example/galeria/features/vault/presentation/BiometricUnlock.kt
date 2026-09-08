package com.example.galeria.features.vault.presentation

import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.galeria.core.AndroidAppContext

actual class BiometricUnlock actual constructor() {

    private var prompt: BiometricPrompt? = null
    private var authGeneration: Int = 0

    actual fun isAvailable(): Boolean {
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
        val current = prompt
        prompt = null
        runCatching { current?.cancelAuthentication() }
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
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MainHandler.post { authenticate(title, subtitle, onResult) }
            return
        }
        val generation = authGeneration + 1
        authGeneration = generation
        runCatching { prompt?.cancelAuthentication() }
        prompt = null
        try {
            val executor = ContextCompat.getMainExecutor(activity)
            val next = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (generation != authGeneration) return
                        prompt = null
                        onResult(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (generation != authGeneration) return
                        prompt = null
                        onResult(false)
                    }
                },
            )
            prompt = next
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Usar PIN")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK,
                )
                .setConfirmationRequired(false)
                .build()
            next.authenticate(info)
        } catch (_: Exception) {
            prompt = null
            if (generation == authGeneration) onResult(false)
        }
    }

    private fun usableActivity(): FragmentActivity? {
        val activity = AndroidAppContext.activity as? FragmentActivity ?: return null
        if (activity.isFinishing || activity.isDestroyed) return null
        return activity
    }

    private companion object {
        val FLAG = byteArrayOf(0x42)
        val MainHandler = Handler(Looper.getMainLooper())
    }
}
