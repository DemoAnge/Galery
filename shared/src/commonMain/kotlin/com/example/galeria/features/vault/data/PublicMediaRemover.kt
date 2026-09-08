package com.example.galeria.features.vault.data

/**
 * Lee y elimina el medio del almacenamiento público (MediaStore / PHPhotoLibrary).
 * [readBytes] no borra; [delete] lo quita de verdad, no solo lo oculta.
 */
expect class PublicMediaRemover() {
    fun readBytes(publicUri: String): ByteArray
    fun delete(publicUri: String)
    fun deleteAll(publicUris: List<String>)
}
