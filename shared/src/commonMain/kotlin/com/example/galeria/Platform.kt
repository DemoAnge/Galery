package com.example.galeria

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform