package com.example.galeria

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.galeria.core.AndroidAppContext

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAppContext.install(applicationContext)
        AndroidAppContext.attachUi(this)
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidAppContext.activity = this
        AndroidAppContext.notifyUiResumed()
    }

    override fun onDestroy() {
        AndroidAppContext.detachUi(this)
        super.onDestroy()
    }
}
