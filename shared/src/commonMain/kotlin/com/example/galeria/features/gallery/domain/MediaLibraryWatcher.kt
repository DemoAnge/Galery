package com.example.galeria.features.gallery.domain

/**
 * Avisa cuando el almacenamiento público cambia (foto/video nuevo, borrado o editado).
 * No hace polling: solo reacciona a MediaStore / PHPhotoLibrary y a volver a primer plano.
 */
expect class MediaLibraryWatcher() {
    fun start(onChanged: () -> Unit)
    fun stop()
}
