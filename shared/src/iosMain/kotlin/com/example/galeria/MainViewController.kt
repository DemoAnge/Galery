package com.example.galeria

import androidx.compose.ui.window.ComposeUIViewController
import com.example.galeria.features.share.presentation.IosShareHost
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val controller = ComposeUIViewController { App() }
    IosShareHost.controller = controller
    return controller
}