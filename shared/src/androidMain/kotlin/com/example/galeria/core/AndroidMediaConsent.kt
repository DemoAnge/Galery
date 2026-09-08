package com.example.galeria.core

import android.app.Activity
import android.content.IntentSender
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lanza el aviso del sistema para borrar medios. El launcher se registra
 * en onCreate; si se registra al vuelo, al abrir el diálogo la Activity
 * ya no está STARTED y falla.
 */
internal object AndroidMediaConsent {

    private val gate = Any()
    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var onResult: ((Boolean) -> Unit)? = null

    fun bind(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            val callback = onResult
            onResult = null
            callback?.invoke(result.resultCode == Activity.RESULT_OK)
        }
        AndroidAppContext.activity = activity
    }

    fun unbind(activity: ComponentActivity) {
        if (AndroidAppContext.activity === activity) {
            AndroidAppContext.activity = null
        }
        onResult?.invoke(false)
        onResult = null
        launcher = null
    }

    fun request(sender: IntentSender): Boolean {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Public media delete denied" }
        synchronized(gate) {
            val latch = CountDownLatch(1)
            val granted = AtomicBoolean(false)
            val posted = Handler(Looper.getMainLooper()).post {
                val current = launcher
                if (current == null) {
                    latch.countDown()
                    return@post
                }
                onResult = { ok ->
                    granted.set(ok)
                    latch.countDown()
                }
                try {
                    current.launch(IntentSenderRequest.Builder(sender).build())
                } catch (_: Exception) {
                    onResult = null
                    latch.countDown()
                }
            }
            if (!posted) return false
            val finished = latch.await(120, TimeUnit.SECONDS)
            return finished && granted.get()
        }
    }
}
