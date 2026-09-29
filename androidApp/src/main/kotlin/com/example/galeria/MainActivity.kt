package com.example.galeria

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.galeria.core.AndroidAppContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val bar = if (night) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
        super.onCreate(savedInstanceState)
        AndroidAppContext.install(applicationContext)
        AndroidAppContext.attachUi(this)
        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        AndroidAppContext.onUiResumed(this)
    }

    override fun onDestroy() {
        AndroidAppContext.detachUi(this)
        super.onDestroy()
    }
}
