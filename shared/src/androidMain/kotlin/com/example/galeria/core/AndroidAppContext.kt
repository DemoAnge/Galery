package com.example.galeria.core

import android.content.Context
import androidx.activity.ComponentActivity
import java.util.concurrent.CopyOnWriteArrayList

object AndroidAppContext {
    @Volatile
    private var app: Context? = null

    @Volatile
    var activity: ComponentActivity? = null

    private val resumeListeners = CopyOnWriteArrayList<() -> Unit>()

    fun install(context: Context) {
        app = context.applicationContext
    }

    fun attachUi(host: ComponentActivity) {
        activity = host
        AndroidMediaConsent.bind(host)
    }

    fun detachUi(host: ComponentActivity) {
        AndroidMediaConsent.unbind(host)
    }

    fun onUiResumed(host: ComponentActivity) {
        activity = host
        notifyUiResumed()
    }

    fun addResumeListener(listener: () -> Unit) {
        resumeListeners += listener
    }

    fun removeResumeListener(listener: () -> Unit) {
        resumeListeners -= listener
    }

    fun notifyUiResumed() {
        resumeListeners.forEach { it() }
    }

    fun require(): Context =
        app ?: error("AndroidAppContext.install() must be called first")
}
